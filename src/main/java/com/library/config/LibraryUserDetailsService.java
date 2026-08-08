package com.library.config;

import java.util.Locale;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import com.library.domain.Member;
import com.library.repository.MemberRepository;

/**
 * Bridges the member table to Spring Security's form login (proposal 3.3).
 * The username is the email, normalized the same way registration
 * normalizes it, and the stored BCrypt hash is what login verifies against.
 *
 * <p>Suspended members may still sign in — suspension blocks borrowing
 * (enforced in LoanService), not looking at your own history.
 */
@Component
public class LibraryUserDetailsService implements UserDetailsService {

    private final MemberRepository members;

    public LibraryUserDetailsService(MemberRepository members) {
        this.members = members;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        Member member = members.findByEmail(normalized)
                .orElseThrow(() -> new UsernameNotFoundException("No account for " + normalized));
        return User.withUsername(member.getEmail())
                .password(member.getPasswordHash())
                .roles(member.getRole().name())
                .build();
    }
}
