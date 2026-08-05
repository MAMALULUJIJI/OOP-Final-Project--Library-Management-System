package com.library.service;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * What a librarian may type when adding or editing a title — and nothing
 * more. Deliberately not the {@code Book} entity: binding the entity to the
 * form would let a crafted request set {@code availableCopies} directly.
 */
public class BookForm {

    @NotBlank(message = "Title is required")
    @Size(max = 255)
    private String title;

    @NotBlank(message = "Author is required")
    @Size(max = 255)
    private String author;

    @NotBlank(message = "ISBN is required")
    @Size(max = 20)
    private String isbn;

    @Size(max = 100)
    private String category;

    @NotNull(message = "Total copies is required")
    @Min(value = 1, message = "A title needs at least one copy")
    private Integer totalCopies;

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

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Integer getTotalCopies() {
        return totalCopies;
    }

    public void setTotalCopies(Integer totalCopies) {
        this.totalCopies = totalCopies;
    }
}
