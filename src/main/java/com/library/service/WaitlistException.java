package com.library.service;

/** A waitlist request that the rules in proposal 2.5 refuse. */
public class WaitlistException extends RuntimeException {

    public WaitlistException(String message) {
        super(message);
    }
}
