/**
 * Data access: one Spring Data repository per entity — {@code BookRepository},
 * {@code MemberRepository}, {@code LoanRepository}, {@code WaitlistEntryRepository}.
 *
 * <p>Rule two of the layering: no queries outside this package. Derived query
 * methods and {@code @Query} both live here; a service composes repository calls
 * but does not write JPQL of its own.
 *
 * <p>Repositories decide nothing. They read and write rows.
 */
package com.library.repository;
