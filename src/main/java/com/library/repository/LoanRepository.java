package com.library.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.library.domain.Book;
import com.library.domain.Loan;
import com.library.domain.Member;

/**
 * Data access for {@link Loan}. "Open loan" always means
 * {@code returnDate IS NULL} — there is no status column to drift.
 */
public interface LoanRepository extends JpaRepository<Loan, Long> {

    /** A member's full ledger, newest first, open and closed alike. */
    List<Loan> findByMemberOrderByBorrowDateDescIdDesc(Member member);

    /** The backstop for "a title cannot be removed while a copy is out". */
    boolean existsByBookAndReturnDateIsNull(Book book);

    /** Removing a title takes its circulation records with it. */
    void deleteByBook(Book book);
}
