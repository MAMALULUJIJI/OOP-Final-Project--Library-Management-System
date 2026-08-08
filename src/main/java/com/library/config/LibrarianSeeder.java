package com.library.config;

import java.time.LocalDate;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.library.domain.Member;
import com.library.domain.MemberStatus;
import com.library.domain.Role;
import com.library.repository.MemberRepository;

/**
 * Creates the first LIBRARIAN account at startup if none exists — resolving
 * proposal open question 6 in favor of seeding over self-registration, so
 * the staff area is never unreachable. Every later librarian or member is
 * registered through the app by an existing librarian.
 *
 * <p>Credentials come from {@code library.librarian.*}, overridable in
 * production with the LIBRARY_LIBRARIAN_EMAIL / LIBRARY_LIBRARIAN_PASSWORD
 * environment variables. The development default is logged with a warning
 * so nobody ships it by accident.
 */
@Component
public class LibrarianSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(LibrarianSeeder.class);

    private final MemberRepository members;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;

    public LibrarianSeeder(MemberRepository members,
                           PasswordEncoder passwordEncoder,
                           @Value("${library.librarian.email}") String email,
                           @Value("${library.librarian.password}") String password) {
        this.members = members;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(String... args) {
        if (members.existsByRole(Role.LIBRARIAN)) {
            return;
        }
        Member librarian = new Member();
        librarian.setName("Head Librarian");
        librarian.setEmail(email.trim().toLowerCase(Locale.ROOT));
        librarian.setPasswordHash(passwordEncoder.encode(password));
        librarian.setRole(Role.LIBRARIAN);
        librarian.setJoinDate(LocalDate.now());
        librarian.setStatus(MemberStatus.ACTIVE);
        members.save(librarian);
        log.warn("Seeded first librarian account '{}'. If this is the development "
                + "default password, change it before anyone else can reach this server.", email);
    }
}
