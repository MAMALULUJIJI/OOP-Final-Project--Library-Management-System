/**
 * Business rules: {@code BookService}, {@code MemberService}, {@code LoanService},
 * {@code WaitlistService}.
 *
 * <p>All validation and every multi-step operation lives here. This is the only
 * layer that knows a borrow means <em>both</em> inserting a loan <em>and</em>
 * decrementing a copy count, or that a return promotes the front of the waitlist
 * to {@code READY}. Those operations are annotated {@code @Transactional} so the
 * writes commit or roll back together.
 *
 * <p>Services depend on repositories and entities. They never see an
 * {@code HttpServletRequest}.
 */
package com.library.service;
