package com.example.bankcards.service;

import com.example.bankcards.config.JwtProperties;
import io.jsonwebtoken.Jwts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    private final JwtProperties props;
    private final SecretKey key;

    public JwtService(JwtProperties props, SecretKey key) {
        this.props = props;
        this.key = key;
    }

    public String generate(String subject, Map<String, Object> claims) {
        log.debug("Generating JWT for subject: {} with claims: {}", subject, claims);
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(props.getExpirationMinutes() * 60L);
        String token = Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
        log.info("JWT generated successfully for subject: {}, expires at: {}", subject, exp);
        return token;
    }

    public String extractUsername(String token) {
        log.debug("Extracting username from JWT token");
        try {
            String username = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject();
            log.info("Username extracted from token: {}", username);
            return username;
        } catch (Exception e) {
            log.error("Failed to extract username from token: {}", e.getMessage());
            throw e;
        }
    }

    public boolean isValid(String token) {
        log.debug("Validating JWT token");
        try {
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            log.info("JWT token is valid");
            return true;
        } catch (Exception e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }
}
