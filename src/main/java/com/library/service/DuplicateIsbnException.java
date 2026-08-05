package com.library.service;

/**
 * Thrown when a create or update would give two catalog entries the same
 * ISBN. The controller turns this into a field error on the form rather
 * than an error page.
 */
public class DuplicateIsbnException extends RuntimeException {

    public DuplicateIsbnException(String isbn) {
        super("The catalog already has a title with ISBN " + isbn);
    }
}
