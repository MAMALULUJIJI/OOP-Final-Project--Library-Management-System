package com.library.service;

/** No such loan — or not this member's loan, which callers treat the same. */
public class LoanNotFoundException extends RuntimeException {

    public LoanNotFoundException(Long id) {
        super("No loan with id " + id + " on your record.");
    }
}
