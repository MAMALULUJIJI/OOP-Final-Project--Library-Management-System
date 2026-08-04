/**
 * Persistence layer: one DAO per entity — {@code BookDAO}, {@code MemberDAO},
 * {@code LoanDAO}, {@code ReservationDAO}.
 *
 * <p>Each exposes plain methods such as {@code findById}, {@code findByCriteria},
 * {@code insert}, {@code update}, {@code delete}, and maps rows to and from the
 * domain classes. All SQL uses {@code PreparedStatement} — never string
 * concatenation.
 *
 * <p>DAOs do not decide anything. A DAO method that needs to participate in a
 * multi-step write (borrow, return) takes the {@link java.sql.Connection} as a
 * parameter so the service layer controls the transaction boundary.
 */
package com.library.dao;
