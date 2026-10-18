package io.github.srinithamizh.oxyvault.jwt;

import io.github.srinithamizh.oxyvault.security.UserPrincipal;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Pure token logic: no database access. Checking a token against the current user state
 * is done by the caller with a single user lookup.
 */
@Service
public class JwtService {

    private static final String ROLE_CLAIM = "role";
    private static final String TOKEN_VERSION_CLAIM = "token_version";

    private final SecretKey secretKey;
    private final long expirationMillis;

    public JwtService(
            @Value("${jwt.secret-key}") String secret,
            @Value("${jwt.expiration}") long expirationMillis) {

        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMillis = expirationMillis;
    }

    public String generateToken(UserPrincipal principal) {
        Date now = new Date();

        return Jwts.builder()
                .subject(principal.getUsername())
                .claim(ROLE_CLAIM, principal.getRole().name())
                .claim(TOKEN_VERSION_CLAIM, principal.getTokenVersion())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMillis))
                .signWith(secretKey)
                .compact();
    }

    /**
     * Verifies signature and expiry and returns the claims.
     *
     * @throws io.jsonwebtoken.JwtException        if the token is invalid, tampered with or expired
     * @throws IllegalArgumentException            if the token is blank
     */
    public Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * A token is only valid while its version matches the user's current version.
     * Logout bumps the version, which revokes every token issued before it.
     */
    public boolean isTokenVersionCurrent(Claims claims, UserPrincipal principal) {
        Long tokenVersion = claims.get(TOKEN_VERSION_CLAIM, Long.class);
        return tokenVersion != null && tokenVersion == principal.getTokenVersion();
    }
}
