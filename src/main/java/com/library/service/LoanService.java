package com.library.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.library.domain.Book;
import com.library.domain.Loan;
import com.library.domain.Member;
import com.library.domain.WaitlistEntry;
import com.library.domain.WaitlistStatus;
import com.library.repository.BookRepository;
import com.library.repository.LoanRepository;

/**
 * Borrow and return (proposal 2.4). Both are multi-step writes — the loan
 * row and the availability count move together or not at all, which is the
 * whole reason these methods are {@code @Transactional}.
 */
@Service
@Transactional(readOnly = true)
public class LoanService {

    private final LoanRepository loans;
    private final BookRepository books;
    private final WaitlistService waitlistService;

    public LoanService(LoanRepository loans, BookRepository books, WaitlistService waitlistService) {
        this.loans = loans;
        this.books = books;
        this.waitlistService = waitlistService;
    }

    /** Whether any copy of a title is still out — the catalog service asks before removal. */
    public boolean hasOpenLoans(Book book) {
        return loans.existsByBookAndReturnDateIsNull(book);
    }

    /** A removed title takes its closed loans with it (called by the catalog service). */
    @Transactional
    public void purgeFor(Book book) {
        loans.deleteByBook(book);
    }

    /** The member's full ledger, newest first. */
    public List<Loan> historyFor(Member member) {
        return loans.findByMemberOrderByBorrowDateDescIdDesc(member);
    }

    /**
     * Borrow a copy: insert the loan and decrement {@code availableCopies}
     * in one transaction. A member holding a READY waitlist spot collects
     * the copy reserved for them (the hold becomes FULFILLED); anyone else
     * gets a copy only if one is on the shelf beyond the outstanding holds.
     */
    @Transactional
    public Loan borrow(Long bookId, Member member) {
        Book book = books.findById(bookId).orElseThrow(() -> new BookNotFoundException(bookId));
        if (member.isSuspended()) {
            throw new BorrowNotAllowedException(
                    "Your card is suspended; see a librarian to reactivate it.");
        }

        waitlistService.expireStaleHolds(book);

        Optional<WaitlistEntry> hold = waitlistService.readyHold(book, member);
        if (hold.isPresent()) {
            hold.get().setStatus(WaitlistStatus.FULFILLED);
        } else if (waitlistService.effectiveAvailable(book) <= 0) {
            throw new BorrowNotAllowedException(book.getAvailableCopies() > 0
                    ? "The remaining copies of \"" + book.getTitle() + "\" are held for the waitlist — join the line."
                    : "No copies of \"" + book.getTitle() + "\" are on the shelf — join the waitlist.");
        }

        // Backstop: no path may take the shelf count negative, including the
        // hold path above, which does not consult effectiveAvailable().
        if (book.getAvailableCopies() <= 0) {
            throw new BorrowNotAllowedException(
                    "No copies of \"" + book.getTitle() + "\" are on the shelf.");
        }
        book.setAvailableCopies(book.getAvailableCopies() - 1);

        Loan loan = new Loan();
        loan.setBook(book);
        loan.setMember(member);
        loan.setBorrowDate(LocalDate.now());
        loan.setDueDate(LocalDate.now().plusDays(CirculationPolicy.LOAN_PERIOD_DAYS));
        return loans.save(loan);
    }

    /**
     * Return a copy: close the loan, put the copy back on the shelf, and
     * promote the front of the waitlist to READY — all one transaction
     * (proposal 2.4/2.5). Members may only return their own loans.
     */
    @Transactional
    public Loan returnLoan(Long loanId, Member member) {
        Loan loan = loans.findById(loanId)
                .filter(l -> l.getMember().getId().equals(member.getId()))
                .orElseThrow(() -> new LoanNotFoundException(loanId));
        if (!loan.isOpen()) {
            throw new IllegalStateException(
                    "\"" + loan.getBook().getTitle() + "\" was already returned.");
        }

        loan.setReturnDate(LocalDate.now());
        Book book = loan.getBook();
        book.setAvailableCopies(book.getAvailableCopies() + 1);
        waitlistService.promoteNext(book);
        return loan;
    }
}
