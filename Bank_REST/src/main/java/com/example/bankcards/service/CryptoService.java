package com.example.bankcards.service;

import com.example.bankcards.config.CryptoProperties;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class CryptoService {

    private static final Logger log = LoggerFactory.getLogger(CryptoService.class);

    private final CryptoProperties props;
    private SecretKey key;
    private static final int GCM_TAG_BITS = 128;
    private static final int IV_BYTES = 12;

    public CryptoService(CryptoProperties props) {
        this.props = props;
    }

    @PostConstruct
    void init() {
        log.debug("Initializing CryptoService...");
        if (props.getAesKeyB64() == null || props.getAesKeyB64().isBlank()) {
            log.warn("AES key not provided in properties. Generating a new one...");
            try {
                KeyGenerator kg = KeyGenerator.getInstance("AES");
                kg.init(256);
                key = kg.generateKey();
                log.info("AES key successfully generated.");
            } catch (Exception e) {
                log.error("AES key generation failed", e);
                throw new IllegalStateException("AES init error", e);
            }
        } else {
            log.debug("AES key provided. Decoding from Base64...");
            byte[] raw = Base64.getDecoder().decode(props.getAesKeyB64());
            key = new SecretKeySpec(raw, "AES");
            log.info("AES key successfully loaded from properties.");
        }
    }

    public String encrypt(String plain) {
        log.debug("Encrypting data... Hash: {}", hash(plain));
        try {
            byte[] iv = new byte[IV_BYTES];
            new SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] ct = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            byte[] out = new byte[iv.length + ct.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(ct, 0, out, iv.length, ct.length);
            String encoded = Base64.getEncoder().encodeToString(out);
            log.debug("Data successfully encrypted. Output length: {} bytes", encoded.length());
            return encoded;
        } catch (Exception e) {
            log.error("Encryption error", e);
            throw new IllegalStateException("Encrypt error", e);
        }
    }

    public String decrypt(String b64) {
        log.debug("Decrypting data... Encoded length: {} bytes", b64.length());
        try {
            byte[] in = Base64.getDecoder().decode(b64);
            byte[] iv = new byte[IV_BYTES];
            byte[] ct = new byte[in.length - IV_BYTES];
            System.arraycopy(in, 0, iv, 0, IV_BYTES);
            System.arraycopy(in, IV_BYTES, ct, 0, ct.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] pt = cipher.doFinal(ct);
            String decrypted = new String(pt, StandardCharsets.UTF_8);
            log.debug("Data successfully decrypted. Hash: {}", hash(decrypted));
            return decrypted;
        } catch (Exception e) {
            log.error("Decryption error", e);
            throw new IllegalStateException("Decrypt error", e);
        }
    }

    private String hash(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            log.error("Hash calculation error", e);
            return "ERROR";
        }
    }
}
