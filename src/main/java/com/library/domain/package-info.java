/**
 * JPA entities: {@code Book}, {@code Member}, {@code Loan}, {@code WaitlistEntry}.
 *
 * <p>Two invariants the schema depends on, worth knowing before writing entities:
 *
 * <ul>
 *   <li><strong>Availability is a count, not a flag.</strong> {@code availableCopies}
 *       moves with every borrow and return. There is no {@code isAvailable} boolean
 *       and adding one guarantees drift.</li>
 *   <li><strong>Overdue is computed, never stored.</strong> Compare the due date to
 *       today at read time. No {@code overdue} column.</li>
 * </ul>
 */
package com.library.domain;
