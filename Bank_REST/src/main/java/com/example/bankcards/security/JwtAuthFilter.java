package com.example.bankcards.security;

import com.example.bankcards.entity.User;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.service.JwtService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwt;
    private final UserDetailsService uds;
    private final UserRepository repo;

    public JwtAuthFilter(JwtService jwt, UserDetailsService uds, UserRepository repo) {
        this.jwt = jwt;
        this.uds = uds;
        this.repo = repo;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        // --- MDC: request id ---
        String reqId = request.getHeader("X-Request-Id");
        if (reqId == null || reqId.isBlank()) {
            reqId = UUID.randomUUID().toString();
        }
        MDC.put("reqId", reqId);

        try {
            String header = request.getHeader(HttpHeaders.AUTHORIZATION);

            if (header == null || !header.startsWith("Bearer ")) {
                log.debug("JwtAuthFilter: Missing or invalid Authorization header format");
                chain.doFilter(request, response);
                return;
            }

            String token = header.substring(7);
            log.debug("JwtAuthFilter: Received token (first 12 chars) = {}...",
                    token.substring(0, Math.min(token.length(), 12)));

            if (!jwt.isValid(token)) {
                log.warn("JwtAuthFilter: Invalid JWT token");
                chain.doFilter(request, response);
                return;
            }

            String username = jwt.extractUsername(token);
            MDC.put("user", username);
            log.debug("JwtAuthFilter: Extracted username from token: {}", username);

            Long userId = repo.findByUsername(username).map(User::getId).orElse(null);
            if (userId == null) {
                log.warn("JwtAuthFilter: userId not found for username='{}' — proceeding with username only", username);
            } else {
                MDC.put("userId", String.valueOf(userId));
            }

            Authentication current = SecurityContextHolder.getContext().getAuthentication();

            if (current != null) {
                log.debug("JwtAuthFilter: Existing authentication found: principal={}", current.getPrincipal());
                boolean samePrincipal = false;
                Object principal = current.getPrincipal();

                if (principal instanceof UserDetails ud) {
                    samePrincipal = username.equals(ud.getUsername());
                } else if (principal instanceof String s) {
                    samePrincipal = username.equals(s);
                }

                if (samePrincipal) {
                    Object d = current.getDetails();
                    boolean needsDetails = (d == null || !(d instanceof Long));
                    if (needsDetails && current instanceof UsernamePasswordAuthenticationToken aat) {
                        aat.setDetails(userId);
                        log.debug("JwtAuthFilter: Added userId={} to the current authentication", userId);
                    }
                } else {
                    log.debug("JwtAuthFilter: Different user in context — replacing with '{}'", username);
                    UserDetails userDetails = uds.loadUserByUsername(username);
                    var authToken = new UsernamePasswordAuthenticationToken(
                            userDetails.getUsername(), null, userDetails.getAuthorities());
                    authToken.setDetails(userId);
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            } else {
                log.debug("JwtAuthFilter: Empty context — setting authentication for '{}'", username);
                UserDetails userDetails = uds.loadUserByUsername(username);
                var authToken = new UsernamePasswordAuthenticationToken(
                        userDetails.getUsername(), null, userDetails.getAuthorities());
                authToken.setDetails(userId);
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }

            chain.doFilter(request, response);
        } finally {
            // clear MDC to avoid leaking data between threads
            MDC.clear();
        }
    }
}
