package com.mku.helpdesk.security;

import com.mku.helpdesk.model.Role;
import com.mku.helpdesk.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Deliberately NOT @SpringBootTest. Loading the full Spring context just
 * to test "does this string get encoded and decoded correctly" is slow
 * (several seconds) and means a typo somewhere unrelated in the codebase
 * can fail this test for a reason that has nothing to do with JWTs.
 *
 * ReflectionTestUtils.setField lets us inject the @Value fields (secret,
 * expirationMs) directly without a running container — that's the "test
 * in isolation" the roadmap is asking for. Run this with:
 *   mvn test -Dtest=JwtUtilTest
 * or just right-click -> Run in your IDE.
 */
class JwtUtilTest {

    private JwtUtil buildUtil() {
        JwtUtil jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", "TEST_ONLY_SECRET_MUST_BE_AT_LEAST_32_CHARS_LONG");
        ReflectionTestUtils.setField(jwtUtil, "expirationMs", 60_000L); // 1 minute, plenty for a test
        return jwtUtil;
    }

    private User sampleStudent() {
        User user = new User();
        user.setId(1L);
        user.setFullName("Jane Student");
        user.setEmail("student@test.com");
        user.setRole(Role.STUDENT);
        return user;
    }

    @Test
    void generatesATokenAndDecodesItBackToTheSameValues() {
        JwtUtil jwtUtil = buildUtil();
        User user = sampleStudent();

        String token = jwtUtil.generateToken(user);
        System.out.println("Generated token: " + token);
        assertNotNull(token);
        // A real JWT has 3 dot-separated segments: header.payload.signature.
        // If this assertion fails, something is wrong with the builder call
        // itself, before you even get to decoding.
        assertEquals(3, token.split("\\.").length);

        String email = jwtUtil.extractEmail(token);
        String role = jwtUtil.extractRole(token);
        Long userId = jwtUtil.extractUserId(token);

        System.out.println("Decoded email: " + email);
        System.out.println("Decoded role: " + role);
        System.out.println("Decoded userId: " + userId);

        assertEquals("student@test.com", email);
        assertEquals("STUDENT", role);
        assertEquals(1L, userId);
        assertFalse(jwtUtil.isTokenExpired(token));
        assertTrue(jwtUtil.isTokenValid(token));
    }

    @Test
    void expiredTokenIsDetectedAsExpired() {
        JwtUtil jwtUtil = buildUtil();
        // Force an expiration that's already in the past.
        ReflectionTestUtils.setField(jwtUtil, "expirationMs", -10_000L);
        String token = jwtUtil.generateToken(sampleStudent());

        assertTrue(jwtUtil.isTokenExpired(token));
    }
}
