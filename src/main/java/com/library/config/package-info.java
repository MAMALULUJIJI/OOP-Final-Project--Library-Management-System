/**
 * Security and application configuration.
 *
 * <p>Home of the {@code SecurityFilterChain} bean, the {@code BCryptPasswordEncoder},
 * and the {@code UserDetailsService} that loads a member by email. Roles are
 * {@code LIBRARIAN} and {@code MEMBER}; catalog and member management are
 * restricted to {@code LIBRARIAN}.
 *
 * <p>Until a security configuration exists here, Spring Boot's default applies:
 * every URL requires login, with a generated password printed at startup.
 */
package com.library.config;
