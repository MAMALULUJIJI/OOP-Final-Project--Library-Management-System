package com.library.domain;

/**
 * Lifecycle of a waitlist spot (proposal 2.5). ACTIVE means waiting in line;
 * READY means a returned copy is being held for this member; FULFILLED and
 * CANCELLED are terminal and free the member to rejoin later.
 */
public enum WaitlistStatus {
    ACTIVE,
    READY,
    FULFILLED,
    CANCELLED
}
