package com.library.service;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.library.domain.Book;
import com.library.domain.Member;
import com.library.domain.WaitlistEntry;
import com.library.domain.WaitlistStatus;
import com.library.repository.BookRepository;
import com.library.repository.WaitlistRepository;

/**
 * The waitlist rules from proposal 2.5.
 *
 * <p>The availability model: returning a book puts the copy back on the
 * shelf ({@code availableCopies} goes up) <em>and</em> promotes the front of
 * the line to READY in the same transaction. A READY hold means one of the
 * shelved copies is reserved, so what other members may borrow is
 * {@link #effectiveAvailable}: shelved copies minus outstanding holds.
 */
@Service
@Transactional(readOnly = true)
public class WaitlistService {

    /** The statuses that occupy a member's one spot per title. */
    private static final Set<WaitlistStatus> OPEN_STATUSES =
            EnumSet.of(WaitlistStatus.ACTIVE, WaitlistStatus.READY);

    private final WaitlistRepository waitlist;
    private final BookRepository books;

    public WaitlistService(WaitlistRepository waitlist, BookRepository books) {
        this.waitlist = waitlist;
        this.books = books;
    }

    /** Shelved copies nobody has a claim on — what a walk-in may borrow. */
    public int effectiveAvailable(Book book) {
        return book.getAvailableCopies() - reservedCopies(book);
    }

    /**
     * Copies spoken for once stale holds have been retired. A hold past the
     * collection window is not simply dropped: expiring it promotes the next
     * member in line, so the copy stays reserved whenever somebody is waiting
     * behind it. Counting only live holds made the book page offer a Borrow
     * button that borrowing then refused.
     */
    public int reservedCopies(Book book) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(CirculationPolicy.HOLD_PERIOD_DAYS);
        long live = waitlist.countByBookAndStatusAndReadyAtAfter(book, WaitlistStatus.READY, cutoff);
        long stale = waitlist.countByBookAndStatus(book, WaitlistStatus.READY) - live;
        long queued = waitlist.countByBookAndStatus(book, WaitlistStatus.ACTIVE);
        return (int) (live + Math.min(stale, queued));
    }

    /** A removed title takes its queue with it (called by the catalog service). */
    @Transactional
    public void purgeFor(Book book) {
        waitlist.deleteByBook(book);
    }

    public Optional<WaitlistEntry> readyHold(Book book, Member member) {
        return waitlist.findFirstByBookAndMemberAndStatus(book, member, WaitlistStatus.READY);
    }

    public Optional<WaitlistEntry> activeEntry(Book book, Member member) {
        return waitlist.findFirstByBookAndMemberAndStatus(book, member, WaitlistStatus.ACTIVE);
    }

    /** First-come, first-served position in line, starting at 1. */
    public int positionOf(WaitlistEntry entry) {
        return (int) waitlist.countByBookAndStatusAndJoinedAtLessThan(
                entry.getBook(), WaitlistStatus.ACTIVE, entry.getJoinedAt()) + 1;
    }

    /** Everything the member is waiting for, oldest first, for the account page. */
    public List<WaitlistStanding> standingsFor(Member member) {
        return waitlist.findByMemberAndStatusInOrderByJoinedAtAsc(member, OPEN_STATUSES).stream()
                .map(entry -> new WaitlistStanding(
                        entry,
                        entry.isReady() ? 0 : positionOf(entry),
                        (int) waitlist.countByBookAndStatus(entry.getBook(), WaitlistStatus.ACTIVE),
                        entry.isReady()
                                ? entry.getReadyAt().plusDays(CirculationPolicy.HOLD_PERIOD_DAYS)
                                : null))
                .toList();
    }

    /**
     * Join a title's line. Permitted only when no copy is effectively
     * available, and only once per member per title (proposal 2.5).
     */
    @Transactional
    public WaitlistEntry join(Long bookId, Member member) {
        Book book = books.findById(bookId).orElseThrow(() -> new BookNotFoundException(bookId));
        expireStaleHolds(book);

        if (member.isSuspended()) {
            // Suspended members cannot borrow, so a hold would only sit at
            // the desk and expire — refuse up front with a readable reason.
            throw new WaitlistException("Your card is suspended; see a librarian to reactivate it.");
        }
        if (waitlist.existsByBookAndMemberAndStatusIn(book, member, OPEN_STATUSES)) {
            throw new WaitlistException("You already hold a spot in line for \"" + book.getTitle() + "\".");
        }
        if (effectiveAvailable(book) > 0) {
            throw new WaitlistException("A copy of \"" + book.getTitle() + "\" is on the shelf — borrow it instead.");
        }

        WaitlistEntry entry = new WaitlistEntry();
        entry.setBook(book);
        entry.setMember(member);
        entry.setJoinedAt(LocalDateTime.now());
        entry.setStatus(WaitlistStatus.ACTIVE);
        return waitlist.save(entry);
    }

    /**
     * Leave the line. Cancelling a READY hold frees the reserved copy, so
     * the next member in line is promoted in the same transaction.
     */
    @Transactional
    public WaitlistEntry cancel(Long entryId, Member member) {
        WaitlistEntry entry = waitlist.findById(entryId)
                .filter(e -> e.getMember().getId().equals(member.getId()))
                .orElseThrow(() -> new WaitlistException("No such spot on your waitlists."));
        if (entry.getStatus() != WaitlistStatus.ACTIVE && entry.getStatus() != WaitlistStatus.READY) {
            throw new WaitlistException("That waitlist spot is already closed.");
        }

        boolean freesACopy = entry.isReady();
        entry.setStatus(WaitlistStatus.CANCELLED);
        if (freesACopy) {
            promoteNext(entry.getBook());
        }
        return entry;
    }

    /**
     * Hand the copy that just became free to the front of the line, if
     * anyone is waiting. Called inside the return transaction (proposal
     * 2.5: promotion happens with the return, or not at all).
     */
    @Transactional
    public void promoteNext(Book book) {
        waitlist.findFirstByBookAndStatusOrderByJoinedAtAsc(book, WaitlistStatus.ACTIVE)
                .ifPresent(next -> {
                    next.setStatus(WaitlistStatus.READY);
                    next.setReadyAt(LocalDateTime.now());
                });
    }

    /**
     * Expire READY holds that sat uncollected past the hold period; each
     * expired hold passes its copy to the next member in line. Run lazily
     * at the top of borrow and join rather than from a scheduler — the
     * moment staleness matters is the moment someone wants the copy.
     */
    @Transactional
    public void expireStaleHolds(Book book) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(CirculationPolicy.HOLD_PERIOD_DAYS);
        for (WaitlistEntry stale : waitlist.findByBookAndStatusAndReadyAtBefore(book, WaitlistStatus.READY, cutoff)) {
            stale.setStatus(WaitlistStatus.CANCELLED);
            promoteNext(book);
        }
    }
}
