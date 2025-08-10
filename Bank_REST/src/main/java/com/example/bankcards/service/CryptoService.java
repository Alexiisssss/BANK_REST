package com.example.bankcards.service;

import com.example.bankcards.config.CryptoProperties;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class CryptoService {

    private final CryptoProperties props;
    private SecretKey key;
    private static final int GCM_TAG_BITS = 128;
    private static final int IV_BYTES = 12;

    public CryptoService(CryptoProperties props) {
        this.props = props;
    }

    @PostConstruct
    void init() {
        if (props.getAesKeyB64() == null || props.getAesKeyB64().isBlank()) {
            try {
                KeyGenerator kg = KeyGenerator.getInstance("AES");
                kg.init(256);
                key = kg.generateKey();
            } catch (Exception e) {
                throw new IllegalStateException("AES init error", e);
            }
        } else {
            byte[] raw = Base64.getDecoder().decode(props.getAesKeyB64());
            key = new SecretKeySpec(raw, "AES");
        }
    }

    public String encrypt(String plain) {
        try {
            byte[] iv = new byte[IV_BYTES];
            new SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] ct = cipher.doFinal(plain.getBytes());
            // формат: base64(iv || ct)
            byte[] out = new byte[iv.length + ct.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(ct, 0, out, iv.length, ct.length);
            return Base64.getEncoder().encodeToString(out);
        } catch (Exception e) {
            throw new IllegalStateException("Encrypt error", e);
        }
    }

    public String decrypt(String b64) {
        try {
            byte[] in = Base64.getDecoder().decode(b64);
            byte[] iv = new byte[IV_BYTES];
            byte[] ct = new byte[in.length - IV_BYTES];
            System.arraycopy(in, 0, iv, 0, IV_BYTES);
            System.arraycopy(in, IV_BYTES, ct, 0, ct.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] pt = cipher.doFinal(ct);
            return new String(pt);
        } catch (Exception e) {
            throw new IllegalStateException("Decrypt error", e);
        }
    }
}
