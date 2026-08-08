package com.library.service;

import java.time.LocalDateTime;

import com.library.domain.WaitlistEntry;

/**
 * One row of the account page's waitlist table: the entry plus the numbers
 * the template shows. {@code position} is 0 for READY holds (they are past
 * the line); {@code heldUntil} is null for entries still waiting.
 */
public record WaitlistStanding(WaitlistEntry entry, int position, int queueLength, LocalDateTime heldUntil) {
}
