package com.library.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.library.domain.Member;
import com.library.domain.Role;

/**
 * Data access for {@link Member}. Emails are normalized to lower case by
 * the service before they get here, so exact-match queries are sufficient.
 */
public interface MemberRepository extends JpaRepository<Member, Long> {

    boolean existsByEmail(String email);

    /** Lets the seeder ask "does any librarian exist yet?". */
    boolean existsByRole(Role role);

    /** Login will resolve the signed-in user through this. */
    Optional<Member> findByEmail(String email);

    /** The member register, alphabetically. */
    List<Member> findAllByOrderByNameAsc();
}
