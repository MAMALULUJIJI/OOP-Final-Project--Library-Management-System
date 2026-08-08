package com.library.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.library.domain.Book;
import com.library.domain.Loan;
import com.library.domain.Member;
import com.library.domain.MemberStatus;
import com.library.domain.Role;
import com.library.domain.WaitlistEntry;
import com.library.domain.WaitlistStatus;
import com.library.repository.BookRepository;
import com.library.repository.LoanRepository;
import com.library.repository.MemberRepository;
import com.library.repository.WaitlistRepository;

/**
 * The circulation rules from proposal 2.4–2.6: borrow and return move the
 * loan row and the availability count together, and the waitlist promotes,
 * reserves, and expires the way section 2.5 says it must.
 */
@SpringBootTest
@Transactional
class CirculationTest {

    @Autowired
    private LoanService loanService;
    @Autowired
    private WaitlistService waitlistService;
    @Autowired
    private BookService bookService;
    @Autowired
    private BookRepository books;
    @Autowired
    private MemberRepository members;
    @Autowired
    private LoanRepository loans;
    @Autowired
    private WaitlistRepository waitlist;

    private int seq = 0;

    private Book shelve(int copies) {
        Book book = new Book();
        book.setIsbn("isbn-" + (++seq));
        book.setTitle("Title " + seq);
        book.setAuthor("Author " + seq);
        book.setTotalCopies(copies);
        book.setAvailableCopies(copies);
        return books.save(book);
    }

    private Member card(MemberStatus status) {
        Member member = new Member();
        member.setName("Member " + (++seq));
        member.setEmail("member" + seq + "@example.edu");
        member.setPasswordHash("x".repeat(60));
        member.setRole(Role.MEMBER);
        member.setJoinDate(LocalDate.now());
        member.setStatus(status);
        return members.save(member);
    }

    // ---------- borrow ----------

    @Test
    void borrowCreatesLoanAndDecrementsAvailability() {
        Book book = shelve(2);
        Member member = card(MemberStatus.ACTIVE);

        Loan loan = loanService.borrow(book.getId(), member);

        assertEquals(LocalDate.now(), loan.getBorrowDate());
        assertEquals(LocalDate.now().plusDays(CirculationPolicy.LOAN_PERIOD_DAYS), loan.getDueDate());
        assertTrue(loan.isOpen());
        assertEquals(1, book.getAvailableCopies());
    }

    @Test
    void suspendedMemberCannotBorrow() {
        Book book = shelve(1);
        Member member = card(MemberStatus.SUSPENDED);

        assertThrows(BorrowNotAllowedException.class, () -> loanService.borrow(book.getId(), member));
        assertEquals(1, book.getAvailableCopies());
    }

    @Test
    void borrowRefusedWhenNoCopyOnTheShelf() {
        Book book = shelve(1);
        loanService.borrow(book.getId(), card(MemberStatus.ACTIVE));

        assertThrows(BorrowNotAllowedException.class,
                () -> loanService.borrow(book.getId(), card(MemberStatus.ACTIVE)));
    }

    // ---------- return ----------

    @Test
    void returnClosesLoanAndRestocksTheShelf() {
        Book book = shelve(1);
        Member member = card(MemberStatus.ACTIVE);
        Loan loan = loanService.borrow(book.getId(), member);

        loanService.returnLoan(loan.getId(), member);

        assertFalse(loan.isOpen());
        assertEquals(LocalDate.now(), loan.getReturnDate());
        assertEquals(1, book.getAvailableCopies());
    }

    @Test
    void memberCannotReturnSomeoneElsesLoan() {
        Book book = shelve(1);
        Loan loan = loanService.borrow(book.getId(), card(MemberStatus.ACTIVE));

        assertThrows(LoanNotFoundException.class,
                () -> loanService.returnLoan(loan.getId(), card(MemberStatus.ACTIVE)));
    }

    // ---------- waitlist ----------

    @Test
    void joinRefusedWhileACopyIsAvailable() {
        Book book = shelve(1);

        assertThrows(WaitlistException.class,
                () -> waitlistService.join(book.getId(), card(MemberStatus.ACTIVE)));
    }

    @Test
    void oneSpotPerMemberPerTitle() {
        Book book = shelve(1);
        loanService.borrow(book.getId(), card(MemberStatus.ACTIVE));
        Member waiting = card(MemberStatus.ACTIVE);

        waitlistService.join(book.getId(), waiting);

        assertThrows(WaitlistException.class, () -> waitlistService.join(book.getId(), waiting));
    }

    @Test
    void returnPromotesTheFrontOfTheLine() {
        Book book = shelve(1);
        Member borrower = card(MemberStatus.ACTIVE);
        Member first = card(MemberStatus.ACTIVE);
        Member second = card(MemberStatus.ACTIVE);
        Loan loan = loanService.borrow(book.getId(), borrower);
        WaitlistEntry firstEntry = waitlistService.join(book.getId(), first);
        WaitlistEntry secondEntry = waitlistService.join(book.getId(), second);
        assertEquals(1, waitlistService.positionOf(firstEntry));
        assertEquals(2, waitlistService.positionOf(secondEntry));

        loanService.returnLoan(loan.getId(), borrower);

        assertEquals(WaitlistStatus.READY, firstEntry.getStatus());
        assertNotNull(firstEntry.getReadyAt());
        assertEquals(WaitlistStatus.ACTIVE, secondEntry.getStatus());
        assertEquals(1, waitlistService.positionOf(secondEntry));
    }

    @Test
    void readyHoldIsReservedFromOtherMembers() {
        Book book = shelve(1);
        Member borrower = card(MemberStatus.ACTIVE);
        Member holder = card(MemberStatus.ACTIVE);
        Loan loan = loanService.borrow(book.getId(), borrower);
        WaitlistEntry hold = waitlistService.join(book.getId(), holder);
        loanService.returnLoan(loan.getId(), borrower);

        // The copy is on the shelf, but reserved: a walk-in cannot take it.
        assertEquals(1, book.getAvailableCopies());
        assertEquals(0, waitlistService.effectiveAvailable(book));
        assertThrows(BorrowNotAllowedException.class,
                () -> loanService.borrow(book.getId(), card(MemberStatus.ACTIVE)));

        // The holder can — and collecting it fulfils the hold.
        loanService.borrow(book.getId(), holder);
        assertEquals(WaitlistStatus.FULFILLED, hold.getStatus());
        assertEquals(0, book.getAvailableCopies());
    }

    @Test
    void cancellingAReadyHoldPassesTheCopyToTheNextInLine() {
        Book book = shelve(1);
        Member borrower = card(MemberStatus.ACTIVE);
        Member first = card(MemberStatus.ACTIVE);
        Member second = card(MemberStatus.ACTIVE);
        Loan loan = loanService.borrow(book.getId(), borrower);
        WaitlistEntry firstEntry = waitlistService.join(book.getId(), first);
        WaitlistEntry secondEntry = waitlistService.join(book.getId(), second);
        loanService.returnLoan(loan.getId(), borrower);

        waitlistService.cancel(firstEntry.getId(), first);

        assertEquals(WaitlistStatus.CANCELLED, firstEntry.getStatus());
        assertEquals(WaitlistStatus.READY, secondEntry.getStatus());
    }

    @Test
    void staleHoldExpiresAndPassesToTheNextInLine() {
        Book book = shelve(1);
        Member borrower = card(MemberStatus.ACTIVE);
        Member first = card(MemberStatus.ACTIVE);
        Member second = card(MemberStatus.ACTIVE);
        Loan loan = loanService.borrow(book.getId(), borrower);
        WaitlistEntry firstEntry = waitlistService.join(book.getId(), first);
        WaitlistEntry secondEntry = waitlistService.join(book.getId(), second);
        loanService.returnLoan(loan.getId(), borrower);

        // Backdate the hold past the expiry window, then let expiry run.
        firstEntry.setReadyAt(LocalDateTime.now().minusDays(CirculationPolicy.HOLD_PERIOD_DAYS + 1));
        waitlistService.expireStaleHolds(book);

        assertEquals(WaitlistStatus.CANCELLED, firstEntry.getStatus());
        assertEquals(WaitlistStatus.READY, secondEntry.getStatus());
    }

    // ---------- catalog interplay ----------

    @Test
    void titleCannotBeRemovedWhileACopyIsOut() {
        Book book = shelve(1);
        loanService.borrow(book.getId(), card(MemberStatus.ACTIVE));

        assertThrows(IllegalStateException.class, () -> bookService.delete(book.getId()));
    }

    @Test
    void removingATitleTakesItsRecordsWithIt() {
        Book book = shelve(1);
        Member member = card(MemberStatus.ACTIVE);
        Loan loan = loanService.borrow(book.getId(), member);
        loanService.returnLoan(loan.getId(), member);

        bookService.delete(book.getId());

        assertTrue(books.findById(book.getId()).isEmpty());
        assertTrue(loans.findByMemberOrderByBorrowDateDescIdDesc(member).isEmpty());
        assertEquals(0, waitlist.count());
    }
}
