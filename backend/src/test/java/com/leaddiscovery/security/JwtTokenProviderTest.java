package com.leaddiscovery.security;

import com.leaddiscovery.entity.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider tokenProvider;
    private final String testSecret = "SuperSecretKeyForJwtTokenProviderTestingThatIsVeryLongAndSecure1234567890";
    private final long testExpirationMs = 3600000; // 1 hour

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider(testSecret, testExpirationMs);
    }

    @Test
    @DisplayName("Should generate valid JWT token from UserPrincipal and parse claims")
    void testGenerateAndParseToken() {
        UserPrincipal principal = new UserPrincipal(42L, "Jane Doe", "jane@example.com", "pwd", Role.USER, Collections.emptyList());

        String token = tokenProvider.generateTokenFromUserPrincipal(principal);

        assertNotNull(token);
        assertTrue(token.split("\\.").length == 3);
        assertTrue(tokenProvider.validateToken(token));

        assertEquals(42L, tokenProvider.getUserIdFromToken(token));
        assertEquals("jane@example.com", tokenProvider.getEmailFromToken(token));
        assertEquals("USER", tokenProvider.getRoleFromToken(token));
    }

    @Test
    @DisplayName("Should reject invalid or malformed JWT token")
    void testValidateInvalidToken() {
        assertFalse(tokenProvider.validateToken("invalid.token.structure"));
        assertFalse(tokenProvider.validateToken(""));
        assertFalse(tokenProvider.validateToken(null));
    }

    @Test
    @DisplayName("Should reject expired JWT token")
    void testValidateExpiredToken() {
        // Create token provider with 0ms expiration
        JwtTokenProvider expiredProvider = new JwtTokenProvider(testSecret, -1000L);
        UserPrincipal principal = new UserPrincipal(1L, "User", "user@test.com", "pwd", Role.USER, Collections.emptyList());

        String expiredToken = expiredProvider.generateTokenFromUserPrincipal(principal);

        assertFalse(tokenProvider.validateToken(expiredToken));
    }
}
