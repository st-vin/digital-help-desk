package com.mku.helpdesk.service;

import com.mku.helpdesk.exception.VerificationException;
import com.mku.helpdesk.model.EmailVerificationToken;
import com.mku.helpdesk.model.User;
import com.mku.helpdesk.repository.EmailVerificationTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final EmailVerificationTokenRepository tokenRepository;

    @Value("${app.email-verification-token-ttl-hours:24}")
    private long tokenTtlHours;

    @Transactional
    public EmailVerificationToken issueToken(User user) {
        tokenRepository.deleteByUserId(user.getId());

        EmailVerificationToken token = new EmailVerificationToken();
        token.setUser(user);
        token.setToken(generateToken());
        token.setExpiresAt(LocalDateTime.now().plusHours(tokenTtlHours));
        token.setUsedAt(null);

        return tokenRepository.save(token);
    }

    @Transactional
    public void verifyToken(String rawToken) {
        String tokenValue = normalizeToken(rawToken);

        EmailVerificationToken token = tokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new VerificationException("This verification link is invalid."));

        if (token.getUsedAt() != null) {
            throw new VerificationException("This verification link has already been used.");
        }

        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new VerificationException("This verification link has expired. Request a new one.");
        }

        User user = token.getUser();
        if (user.isEmailVerified()) {
            token.setUsedAt(LocalDateTime.now());
            return;
        }

        user.setEmailVerified(true);
        user.setVerifiedAt(LocalDateTime.now());
        token.setUsedAt(LocalDateTime.now());
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String normalizeToken(String token) {
        if (token == null || token.isBlank()) {
            throw new VerificationException("This verification link is invalid.");
        }
        return token.trim();
    }
}
