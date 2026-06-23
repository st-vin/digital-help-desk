package com.mku.helpdesk.event;

public record VerificationEmailRequestedEvent(
        Long userId,
        String fullName,
        String email,
        String token
) {
}
