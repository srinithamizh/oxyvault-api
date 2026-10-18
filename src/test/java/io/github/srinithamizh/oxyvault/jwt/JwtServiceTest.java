package io.github.srinithamizh.oxyvault.jwt;

import io.github.srinithamizh.oxyvault.enums.Role;
import io.github.srinithamizh.oxyvault.security.UserPrincipal;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-key-that-is-at-least-32-bytes-long";

    private final JwtService jwtService = new JwtService(SECRET, 3_600_000L);

    private UserPrincipal principal(long tokenVersion) {
        return new UserPrincipal("alice", "hash", Role.USER, tokenVersion);
    }

    @Test
    void roundTripsClaims() {
        String token = jwtService.generateToken(principal(0));

        Claims claims = jwtService.extractClaims(token);

        assertEquals("alice", claims.getSubject());
        assertEquals("USER", claims.get("role", String.class));
        assertTrue(jwtService.isTokenVersionCurrent(claims, principal(0)));
    }

    @Test
    void tokenIsRevokedWhenVersionChanges() {
        Claims claims = jwtService.extractClaims(jwtService.generateToken(principal(0)));

        // Logout bumps the stored version to 1; the old token (version 0) must stop working.
        assertFalse(jwtService.isTokenVersionCurrent(claims, principal(1)));
    }

    @Test
    void rejectsTamperedToken() {
        String token = jwtService.generateToken(principal(0));
        String tampered = token.substring(0, token.length() - 2) + "xx";

        assertThrows(JwtException.class, () -> jwtService.extractClaims(tampered));
    }

    @Test
    void rejectsTokenSignedWithAnotherKey() {
        JwtService other = new JwtService("another-secret-key-that-is-also-32-bytes-long!!", 3_600_000L);

        String token = other.generateToken(principal(0));

        assertThrows(JwtException.class, () -> jwtService.extractClaims(token));
    }

    @Test
    void rejectsExpiredToken() {
        JwtService shortLived = new JwtService(SECRET, -1_000L);

        String token = shortLived.generateToken(principal(0));

        assertThrows(JwtException.class, () -> jwtService.extractClaims(token));
    }
}
