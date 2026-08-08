package com.library.service;

/**
 * The two circulation constants, in one place per the proposal's advice —
 * change them here, not by hunting literals through the services.
 */
public final class CirculationPolicy {

    /** Due date is borrow date plus this (proposal 2.6). */
    public static final int LOAN_PERIOD_DAYS = 14;

    /**
     * How long a READY hold waits at the desk before it expires and passes
     * to the next member in line. The proposal left this open (question 2);
     * three days keeps the queue moving without punishing a weekend away.
     */
    public static final int HOLD_PERIOD_DAYS = 3;

    private CirculationPolicy() {
    }
}
