package com.library.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Version;

/**
 * A title in the catalog. Availability is a count of copies on the shelf,
 * never a boolean — {@code availableCopies} moves with every borrow and
 * return. See schema.sql for the table this maps to.
 */
@Entity
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String isbn;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String author;

    private String category;

    @Column(nullable = false)
    private int totalCopies;

    @Column(nullable = false)
    private int availableCopies;

    /**
     * Optimistic lock. Two members clicking Borrow on the last copy both read
     * availableCopies = 1; without this the second write silently overwrites
     * the first and one copy becomes two loans. Hibernate checks and bumps this
     * on every update, so the loser gets an OptimisticLockException instead.
     */
    // The column carries a default so that ALTER TABLE on a database written
    // before this field existed backfills 0 instead of NULL — Hibernate throws
    // an NPE when it reads a null version.
    @Version
    @Column(columnDefinition = "bigint default 0")
    private Long version = 0L;

    public Long getId() {
        return id;
    }

    public Long getVersion() {
        return version;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getTotalCopies() {
        return totalCopies;
    }

    public void setTotalCopies(int totalCopies) {
        this.totalCopies = totalCopies;
    }

    public int getAvailableCopies() {
        return availableCopies;
    }

    public void setAvailableCopies(int availableCopies) {
        this.availableCopies = availableCopies;
    }

    /** Copies currently out with members. Derived, never stored. */
    public int getCopiesOnLoan() {
        return totalCopies - availableCopies;
    }
}
