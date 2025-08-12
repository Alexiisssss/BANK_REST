package com.example.bankcards.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {
    /**
     * Secret key for HS256 signature.
     * For production — at least 256 bits.
     */
    private String secret;

    /**
     * Token lifetime in minutes.
     */
    private long expirationMinutes = 60;
}
