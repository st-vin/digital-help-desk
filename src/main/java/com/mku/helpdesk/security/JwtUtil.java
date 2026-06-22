package com.mku.helpdesk.security;

import com.mku.helpdesk.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Generates and validates JWTs using jjwt 0.12.6.
 * <p>
 * In modern JJWT, we use the Keys utility class to generate a secure SecretKey,
 * and standard methods like verifyWith() and parseSignedClaims() to parse tokens.
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration-ms}")
    private long expirationMs;

    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        // JJWT 0.12.x requires keys of appropriate length.
        // HS256 requires a key of at least 256 bits (32 bytes).
        // Keys.hmacShaKeyFor automatically securely wraps your raw secret bytes.
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Builds a token carrying the user's email (as the subject), plus role,
     * userId and fullName as custom claims so the frontend and filter don't
     * need a second database call just to know who they're talking to.
     */
    public String generateToken(User user) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(user.getEmail())
                .claim("role", user.getRole().name())
                .claim("userId", user.getId())
                .claim("fullName", user.getFullName())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSigningKey(), Jwts.SIG.HS256) // Use Jwts.SIG for modern algorithm specification
                .compact();
    }

    /** Throws a runtime exception (caught by the filter, not here) if the token is malformed, expired, or tampered with. */
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey()) // Replaces setSigningKey()
                .build()                     // Must build the parser explicitly now
                .parseSignedClaims(token)    // Replaces parseClaimsJws()
                .getPayload();               // Replaces getBody()
    }

    public String extractEmail(String token) {
        return parseClaims(token).getSubject();
    }

    public String extractRole(String token) {
        return parseClaims(token).get("role", String.class);
    }

    public Long extractUserId(String token) {
        // Claims stores numbers as Integer by default during JSON parsing in
        // some cases — casting straight to Long can throw ClassCastException.
        // .get(key, Long.class) handles the conversion safely either way.
        return parseClaims(token).get("userId", Long.class);
    }

    public boolean isTokenExpired(String token) {
        try {
            return parseClaims(token).getExpiration().before(new Date());
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            return true;
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * True only if the token parses successfully AND is not expired.
     */
    public boolean isTokenValid(String token) {
        try {
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }
}