package com.library.service;

/**
 * Thrown when registering would give two members the same email. Email is
 * the login identifier, so this is an identity clash, not a formatting
 * problem — the controller pins it to the email field on the form.
 */
public class DuplicateEmailException extends RuntimeException {

    public DuplicateEmailException(String email) {
        super("A member is already registered with " + email);
    }
}
