package com.library.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Access rules per proposal 3.3: session-based form login, email as the
 * username, and the staff area behind the LIBRARIAN role. The catalog stays
 * public — browsing requires no account; borrowing does.
 *
 * <p>CSRF stays on — Thymeleaf adds the token to every {@code th:action}
 * form automatically, the login form included.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // The librarian's desk, catalog and member management alike.
                .requestMatchers("/admin/**").hasRole("LIBRARIAN")
                // Circulation: you must be somebody to borrow a book.
                .requestMatchers("/account", "/books/*/borrow", "/books/*/waitlist",
                        "/loans/*/return", "/waitlist/*/cancel").authenticated()
                // Browsing the catalog, the login page, css and js: public.
                .anyRequest().permitAll())
            .formLogin(form -> form
                .loginPage("/login")
                .usernameParameter("email")
                .defaultSuccessUrl("/catalog")
                .permitAll())
            .logout(logout -> logout
                .logoutSuccessUrl("/catalog"));
        return http.build();
    }

    /**
     * BCrypt for member passwords (proposal 3.3). Registration hashes with
     * this, and form login verifies against the same hashes.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
