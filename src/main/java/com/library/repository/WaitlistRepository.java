package com.library.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.library.domain.Book;
import com.library.domain.Member;
import com.library.domain.WaitlistEntry;
import com.library.domain.WaitlistStatus;

/**
 * Data access for {@link WaitlistEntry}. Queue order is always
 * {@code joinedAt} ascending — first come, first served (proposal 2.5).
 */
public interface WaitlistRepository extends JpaRepository<WaitlistEntry, Long> {

    /** The front of a title's line. */
    Optional<WaitlistEntry> findFirstByBookAndStatusOrderByJoinedAtAsc(Book book, WaitlistStatus status);

    long countByBookAndStatus(Book book, WaitlistStatus status);

    /** How many joined before this one — position in line is this plus one. */
    long countByBookAndStatusAndJoinedAtLessThan(Book book, WaitlistStatus status, LocalDateTime joinedAt);

    /** The "one spot per member per title" check, across ACTIVE and READY. */
    boolean existsByBookAndMemberAndStatusIn(Book book, Member member, Collection<WaitlistStatus> statuses);

    Optional<WaitlistEntry> findFirstByBookAndMemberAndStatus(Book book, Member member, WaitlistStatus status);

    /** READY holds still inside the hold window — the ones really reserving a copy. */
    long countByBookAndStatusAndReadyAtAfter(Book book, WaitlistStatus status, LocalDateTime cutoff);

    /** READY holds that have sat uncollected past the cutoff. */
    List<WaitlistEntry> findByBookAndStatusAndReadyAtBefore(Book book, WaitlistStatus status, LocalDateTime cutoff);

    /** Everything a member is currently waiting for, for the account page. */
    List<WaitlistEntry> findByMemberAndStatusInOrderByJoinedAtAsc(Member member, Collection<WaitlistStatus> statuses);

    /** Removing a title dissolves its queue. */
    void deleteByBook(Book book);
}
