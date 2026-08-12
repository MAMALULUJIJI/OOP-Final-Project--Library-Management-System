package com.library.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.library.domain.Book;
import com.library.domain.Member;
import com.library.domain.MemberStatus;
import com.library.domain.Role;
import com.library.domain.WaitlistEntry;
import com.library.domain.WaitlistStatus;
import com.library.repository.BookRepository;
import com.library.repository.MemberRepository;
import com.library.repository.WaitlistRepository;

/**
 * The copy-count arithmetic in {@link BookService#update}, which nothing
 * covered before. The rule under test is the one that matters for the
 * catalogue: a librarian may not reduce a title below what the library has
 * already committed, counting both copies on loan and copies promised to a
 * READY waitlist hold. Missing the second term let availableCopies reach -1.
 */
@SpringBootTest
@Transactional
class CatalogRulesTest {

    @Autowired
    private BookService bookService;
    @Autowired
    private LoanService loanService;
    @Autowired
    private WaitlistService waitlistService;
    @Autowired
    private BookRepository books;
    @Autowired
    private MemberRepository members;
    @Autowired
    private WaitlistRepository waitlist;

    private int seq = 0;

    private Book shelve(int copies) {
        Book book = new Book();
        book.setIsbn("cat-" + (++seq));
        book.setTitle("Catalogue Title " + seq);
        book.setAuthor("Author " + seq);
        book.setTotalCopies(copies);
        book.setAvailableCopies(copies);
        return books.save(book);
    }

    private Member card() {
        Member member = new Member();
        member.setName("Reader " + (++seq));
        member.setEmail("reader" + seq + "@example.edu");
        member.setPasswordHash("x".repeat(60));
        member.setRole(Role.MEMBER);
        member.setJoinDate(LocalDate.now());
        member.setStatus(MemberStatus.ACTIVE);
        return members.save(member);
    }

    private BookForm formFor(Book book, int totalCopies) {
        BookForm form = new BookForm();
        form.setIsbn(book.getIsbn());
        form.setTitle(book.getTitle());
        form.setAuthor(book.getAuthor());
        form.setCategory(book.getCategory());
        form.setTotalCopies(totalCopies);
        return form;
    }

    @Test
    void totalCopiesMayNotDropBelowCopiesOnLoan() {
        Book book = shelve(2);
        loanService.borrow(book.getId(), card());

        assertThrows(IllegalStateException.class,
                () -> bookService.update(book.getId(), formFor(book, 0)));
    }

    /**
     * The regression. One copy, borrowed, queued, then returned — which turns
     * the queued entry into a READY hold reserving that copy. The librarian
     * must not be able to take the title down to zero underneath the hold.
     */
    @Test
    void totalCopiesMayNotDropBelowCopiesReservedByReadyHolds() {
        Book book = shelve(1);
        Member borrower = card();
        Member waiter = card();

        var loan = loanService.borrow(book.getId(), borrower);
        waitlistService.join(book.getId(), waiter);
        loanService.returnLoan(loan.getId(), borrower);

        assertEquals(1, book.getAvailableCopies());
        assertEquals(1, waitlist.countByBookAndStatus(book, WaitlistStatus.READY));
        assertEquals(0, waitlistService.effectiveAvailable(book),
                "the shelved copy is promised to the hold, so nothing is free");

        assertThrows(IllegalStateException.class,
                () -> bookService.update(book.getId(), formFor(book, 0)),
                "reducing the total onto a reserved copy must be refused");
    }

    /** The end of that scenario: collecting a hold lands exactly on zero. */
    @Test
    void collectingAReadyHoldNeverDrivesAvailabilityNegative() {
        Book book = shelve(1);
        Member borrower = card();
        Member waiter = card();

        var loan = loanService.borrow(book.getId(), borrower);
        waitlistService.join(book.getId(), waiter);
        loanService.returnLoan(loan.getId(), borrower);

        loanService.borrow(book.getId(), waiter);

        assertEquals(0, book.getAvailableCopies());
    }

    /**
     * The backstop in {@link LoanService#borrow} itself, exercised directly.
     * A READY hold against an empty shelf is the state the old guard allowed a
     * librarian to create; collecting it is what drove the count to -1.
     */
    @Test
    void borrowRefusesOnTheHoldPathWhenTheShelfIsAlreadyEmpty() {
        Book book = shelve(1);
        Member holder = card();
        loanService.borrow(book.getId(), card());
        assertEquals(0, book.getAvailableCopies());

        WaitlistEntry hold = new WaitlistEntry();
        hold.setBook(book);
        hold.setMember(holder);
        hold.setJoinedAt(LocalDateTime.now());
        hold.setStatus(WaitlistStatus.READY);
        hold.setReadyAt(LocalDateTime.now());
        waitlist.save(hold);

        assertThrows(BorrowNotAllowedException.class,
                () -> loanService.borrow(book.getId(), holder));
        assertEquals(0, book.getAvailableCopies(), "the refusal must not decrement");
    }

    /**
     * A hold past its collection window is not simply free. Expiring it promotes
     * whoever is next, so the copy stays reserved — otherwise the catalogue
     * offers a Borrow button that borrowing refuses.
     */
    @Test
    void aStaleHoldWithSomeoneBehindItKeepsTheCopyReserved() {
        Book book = shelve(1);
        Member borrower = card();
        Member first = card();
        Member second = card();

        var loan = loanService.borrow(book.getId(), borrower);
        waitlistService.join(book.getId(), first);
        waitlistService.join(book.getId(), second);
        loanService.returnLoan(loan.getId(), borrower);

        WaitlistEntry ready = waitlistService.readyHold(book, first).orElseThrow();
        ready.setReadyAt(LocalDateTime.now().minusDays(CirculationPolicy.HOLD_PERIOD_DAYS + 1));

        assertEquals(0, waitlistService.effectiveAvailable(book),
                "expiring the stale hold promotes the next member, so nothing is free");
    }

    /** With nobody waiting behind it, an expired hold really does free the copy. */
    @Test
    void aStaleHoldWithNobodyBehindItReleasesTheCopy() {
        Book book = shelve(1);
        Member borrower = card();
        Member waiter = card();

        var loan = loanService.borrow(book.getId(), borrower);
        waitlistService.join(book.getId(), waiter);
        loanService.returnLoan(loan.getId(), borrower);

        WaitlistEntry ready = waitlistService.readyHold(book, waiter).orElseThrow();
        ready.setReadyAt(LocalDateTime.now().minusDays(CirculationPolicy.HOLD_PERIOD_DAYS + 1));

        assertEquals(1, waitlistService.effectiveAvailable(book));
    }

    /** The form accepts hyphens, so the uniqueness check must ignore them. */
    @Test
    void hyphenatedIsbnIsRecognisedAsTheSameTitle() {
        BookForm plain = new BookForm();
        plain.setIsbn("0306406152");
        plain.setTitle("Probe");
        plain.setAuthor("Author");
        plain.setTotalCopies(1);
        bookService.create(plain);

        BookForm hyphenated = new BookForm();
        hyphenated.setIsbn("0-306-40615-2");
        hyphenated.setTitle("Probe Again");
        hyphenated.setAuthor("Author");
        hyphenated.setTotalCopies(1);

        assertThrows(DuplicateIsbnException.class, () -> bookService.create(hyphenated));
    }

    /** Raising the total puts the new copies straight on the shelf. */
    @Test
    void raisingTotalCopiesAddsThemToTheShelf() {
        Book book = shelve(1);
        loanService.borrow(book.getId(), card());
        assertEquals(0, book.getAvailableCopies());

        bookService.update(book.getId(), formFor(book, 3));

        assertEquals(3, book.getTotalCopies());
        assertEquals(2, book.getAvailableCopies(), "three copies, one still out");
    }
}
