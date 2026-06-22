package com.mku.helpdesk.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * What the client sends to POST /api/v1/auth/login.
 * @Data is safe on DTOs (unlike entities) because DTOs never have JPA
 * relationship fields to recurse through.
 *
 * Remember: these annotations only fire if the controller method
 * signature includes @Valid. Without it, this compiles and runs fine
 * but silently accepts blank emails and passwords — a classic invisible
 * bug. Test it on purpose: submit an empty login form via Postman and
 * confirm you get a 400, not a 200 or a 500.
 */
@Data
public class LoginRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email address")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;
}
