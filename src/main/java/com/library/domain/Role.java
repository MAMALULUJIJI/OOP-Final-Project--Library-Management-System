package com.library.domain;

/**
 * What a signed-in account is allowed to do. Stored as text via
 * {@code @Enumerated(STRING)} — never by ordinal, which would silently
 * re-meaning every row if the constants were ever reordered.
 */
public enum Role {
    MEMBER,
    LIBRARIAN
}
