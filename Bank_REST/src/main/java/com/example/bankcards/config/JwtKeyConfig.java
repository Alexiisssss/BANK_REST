package com.example.bankcards.config;

import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.crypto.SecretKey;

@Configuration
public class JwtKeyConfig {

    @Bean
    public SecretKey jwtSecretKey(JwtProperties props) {
        byte[] raw;
        try {
            raw = Decoders.BASE64.decode(props.getSecret());
        } catch (Exception e) {
            raw = props.getSecret().getBytes();
        }
        return Keys.hmacShaKeyFor(raw);
    }
}
