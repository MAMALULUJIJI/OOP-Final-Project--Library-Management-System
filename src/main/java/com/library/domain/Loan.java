package com.library.domain;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

/**
 * One copy of a title, out with one member. A null {@code returnDate} means
 * the loan is still open; overdue is always computed from {@code dueDate} at
 * read time and never stored (proposal 3.4).
 *
 * <p>No {@code setId()}: the database assigns ids and nothing else may.
 */
@Entity
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @ManyToOne(optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false)
    private LocalDate borrowDate;

    @Column(nullable = false)
    private LocalDate dueDate;

    /** Null while the book is still out. */
    private LocalDate returnDate;

    public Long getId() {
        return id;
    }

    public Book getBook() {
        return book;
    }

    public void setBook(Book book) {
        this.book = book;
    }

    public Member getMember() {
        return member;
    }

    public void setMember(Member member) {
        this.member = member;
    }

    public LocalDate getBorrowDate() {
        return borrowDate;
    }

    public void setBorrowDate(LocalDate borrowDate) {
        this.borrowDate = borrowDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public LocalDate getReturnDate() {
        return returnDate;
    }

    public void setReturnDate(LocalDate returnDate) {
        this.returnDate = returnDate;
    }

    public boolean isOpen() {
        return returnDate == null;
    }

    /** Computed against today, per the "never store overdue" rule. */
    public boolean isOverdue() {
        return isOpen() && dueDate.isBefore(LocalDate.now());
    }

    public long getDaysOverdue() {
        return isOverdue() ? ChronoUnit.DAYS.between(dueDate, LocalDate.now()) : 0;
    }

    /** Closed, but after the due date — the "Returned Late" stamp. */
    public boolean isReturnedLate() {
        return returnDate != null && returnDate.isAfter(dueDate);
    }
}
