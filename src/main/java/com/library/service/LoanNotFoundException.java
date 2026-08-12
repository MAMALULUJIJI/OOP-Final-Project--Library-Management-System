package com.library.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** No such loan — or not this member's loan, which callers treat the same. */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class LoanNotFoundException extends RuntimeException {

    public LoanNotFoundException(Long id) {
        super("No loan with id " + id + " on your record.");
    }
}
