package com.mku.helpdesk.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * What an admin sends to create a new ICT_STAFF account. There is no
 * publicly reachable endpoint that lets someone register themselves as
 * ICT_STAFF or ADMIN — this DTO is only ever used behind the
 * /api/v1/admin/** matcher, which SecurityConfig restricts to
 * hasRole("ADMIN") alone.
 */
@Data
public class CreateStaffRequest {

    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email address")
    private String email;

    @NotBlank(message = "Temporary password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;
}
