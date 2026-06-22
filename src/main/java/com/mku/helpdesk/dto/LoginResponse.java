package com.mku.helpdesk.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * What the server sends back after a successful login. The frontend
 * stores `token` in localStorage and `role` to decide which dashboard
 * to redirect to — see api.js / the role guard pattern from the
 * architecture doc.
 *
 * Deliberately does NOT include passwordHash, id, or anything else from
 * the User entity that the frontend doesn't strictly need.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private String role;
    private String fullName;
    private Long userId;
}
