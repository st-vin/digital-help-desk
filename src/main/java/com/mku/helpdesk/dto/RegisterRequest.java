package com.mku.helpdesk.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * What a student sends to POST /api/v1/auth/register.
 * This endpoint always creates a STUDENT account — there's no role field
 * here, deliberately. ICT_STAFF accounts are created only via
 * AdminController, by an existing admin. If this DTO had a role field,
 * anyone could register themselves as ADMIN by including it in the JSON
 * body — same class of bug as letting a TicketRequest carry a status field.
 */
@Data
public class RegisterRequest {

    @NotBlank(message = "Student number is required")
    private String studentNumber;

    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email address")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    @NotBlank(message = "Please confirm your password")
    private String confirmPassword;
}
