package com.library.service;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * What a librarian types when issuing a library card — and nothing more.
 * Role, status, and join date are decided by the service; a request cannot
 * smuggle in {@code role=LIBRARIAN} because there is no field for it.
 */
public class MemberForm {

    @NotBlank(message = "Name is required")
    @Size(max = 255)
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Not a valid email address")
    @Size(max = 255)
    private String email;

    /** BCrypt ignores input beyond 72 bytes, so cap it there. */
    @NotBlank(message = "A temporary password is required")
    @Size(min = 8, max = 72, message = "Password must be 8–72 characters")
    private String password;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
