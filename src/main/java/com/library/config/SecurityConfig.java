package com.library.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Wide open on purpose, for now: only the catalog exists, and member accounts
 * don't. Replacing Spring Boot's default (every URL behind a generated
 * password) with permit-all makes the book CRUD testable in a browser.
 *
 * TODO(auth): once Member and login land, restrict /admin/** to LIBRARIAN and
 * add form login per proposal section 3.3. CSRF stays on — Thymeleaf forms
 * include the token automatically.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }

    /**
     * BCrypt for member passwords (proposal 3.3). Registration hashes with
     * this today; form login will verify against the same hashes when auth
     * lands, so no member needs re-registering.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
