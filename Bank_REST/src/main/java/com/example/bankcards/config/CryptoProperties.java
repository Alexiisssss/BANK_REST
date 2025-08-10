package com.example.bankcards.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.crypto")
public class CryptoProperties {
    /**
     * Base64-строка 32-байтового ключа (AES-256).
     */
    private String aesKeyB64;

    public String getAesKeyB64() { return aesKeyB64; }
    public void setAesKeyB64(String aesKeyB64) { this.aesKeyB64 = aesKeyB64; }
}
