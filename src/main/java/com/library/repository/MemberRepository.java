package com.library.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.library.domain.Member;

/**
 * Data access for {@link Member}. Emails are normalized to lower case by
 * the service before they get here, so exact-match queries are sufficient.
 */
public interface MemberRepository extends JpaRepository<Member, Long> {

    boolean existsByEmail(String email);

    /** Login will resolve the signed-in user through this. */
    Optional<Member> findByEmail(String email);

    /** The member register, alphabetically. */
    List<Member> findAllByOrderByNameAsc();
}
