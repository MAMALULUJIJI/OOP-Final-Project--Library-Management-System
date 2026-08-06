package com.library.domain;

/**
 * Standing of a library card. Suspended members keep their record and
 * history but cannot borrow (proposal 2.6).
 */
public enum MemberStatus {
    ACTIVE,
    SUSPENDED
}
