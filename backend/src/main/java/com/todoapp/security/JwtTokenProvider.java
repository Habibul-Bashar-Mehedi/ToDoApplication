package com.todoapp.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Provides JWT generation, validation, and claim extraction.
 * Uses HMAC-SHA256 signing with a configurable secret key.
 */
@Slf4j
@Component
public class JwtTokenProvider {

    /** Standard token expiry: 30 minutes (in milliseconds). */
    private static final long STANDARD_EXPIRY_MS = 30L * 60 * 1000;

    /** Remember-Me token expiry: 7 days (in milliseconds). */
    private static final long REMEMBER_ME_EXPIRY_MS = 7L * 24 * 60 * 60 * 1000;

    private final SecretKey signingKey;

    public JwtTokenProvider(@Value("${app.jwt.secret}") String secret) {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Generates a signed JWT for the given user.
     *
     * @param userDetails the authenticated user details (username = email)
     * @param userId      the database ID of the user
     * @param rememberMe  if {@code true}, token expires in 7 days; otherwise 30 minutes
     * @return signed JWT string
     */
    public String generateToken(UserDetails userDetails, Long userId, boolean rememberMe) {
        long expiryMs = rememberMe ? REMEMBER_ME_EXPIRY_MS : STANDARD_EXPIRY_MS;
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expiryMs);

        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        return Jwts.builder()
                .id(UUID.randomUUID().toString())          // jti
                .subject(userDetails.getUsername())         // email as subject
                .claim("userId", userId)
                .claim("email", userDetails.getUsername())
                .claim("roles", roles)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }

    /**
     * Validates the token's signature and expiry.
     *
     * @param token the JWT string
     * @return {@code true} if valid, {@code false} otherwise
     */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Extracts the {@code userId} claim from the token.
     *
     * @param token the JWT string
     * @return the user's database ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = parseClaims(token);
        return claims.get("userId", Long.class);
    }

    /**
     * Extracts the {@code jti} (JWT ID) claim from the token.
     *
     * @param token the JWT string
     * @return the JWT ID string
     */
    public String getJtiFromToken(String token) {
        return parseClaims(token).getId();
    }

    /**
     * Extracts the email (subject) from the token.
     *
     * @param token the JWT string
     * @return the user's email address
     */
    public String getEmailFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * Returns the expiry timestamp (epoch millis) of the token.
     *
     * @param token the JWT string
     * @return expiry as epoch milliseconds
     */
    public long getExpiryMillis(String token) {
        return parseClaims(token).getExpiration().getTime();
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
