/**
 * Business rules: {@code BookService}, {@code MemberService}, {@code LoanService},
 * {@code ReservationService}.
 *
 * <p>All validation and every multi-step operation lives here. This is the only
 * layer that knows a borrow means <em>both</em> inserting a loan <em>and</em>
 * decrementing a copy count, and it is where those two writes are wrapped in a
 * single JDBC transaction.
 *
 * <p>Services depend on the DAO layer and the domain classes. They never touch
 * Swing.
 */
package com.library.service;
