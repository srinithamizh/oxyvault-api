package io.github.srinithamizh.oxyvault.jwt;

import io.github.srinithamizh.oxyvault.entity.User;
import io.github.srinithamizh.oxyvault.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Objects;

@Service
public class JwtService {

    private final SecretKey secretKey;
    private final long expirationTime;
    private final UserRepository userRepository;

    public JwtService(
            @Value("${jwt.secret-key}") String secret,
            @Value("${jwt.expiration}") long expiration,
            UserRepository userRepository) {

        this.secretKey = Keys.hmacShaKeyFor(
                secret.getBytes(StandardCharsets.UTF_8)
        );
        this.expirationTime = expiration;
        this.userRepository = userRepository;
    }

    public String generateToken(User user) {
        return Jwts.builder()
                .subject(user.getUsername())
                .claim("role", user.getRole().name())
                .claim("token_version", user.getTokenVersion())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(secretKey)
                .compact();
    }

    public Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractUsername(String token) {
        return extractClaims(token).getSubject();
    }

    public boolean validateToken(String token) {
        try {
            Claims claims = extractClaims(token);

            String username = claims.getSubject();
            Long tokenVersion = claims.get("token_version", Long.class);

            return userRepository.findByUsername(username)
                    .map(user -> Objects.equals(
                            user.getTokenVersion(),
                            tokenVersion))
                    .orElse(false);

        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}