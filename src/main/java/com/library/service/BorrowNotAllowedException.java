package com.library.service;

/** A borrow request that the circulation rules refuse. */
public class BorrowNotAllowedException extends RuntimeException {

    public BorrowNotAllowedException(String message) {
        super(message);
    }
}
