package com.library.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.library.domain.Book;
import com.library.domain.Member;
import com.library.domain.MemberStatus;
import com.library.domain.Role;
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

    /** The end of that scenario: collecting a hold can never go negative. */
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
        assertTrue(book.getAvailableCopies() >= 0,
                "availableCopies is CHECK (>= 0) in the schema and must stay so");
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
