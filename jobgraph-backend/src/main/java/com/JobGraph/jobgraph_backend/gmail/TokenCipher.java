package com.JobGraph.jobgraph_backend.gmail;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class TokenCipher {
    private final String configuredKey;
    private final SecureRandom random = new SecureRandom();

    public TokenCipher(@Value("${app.gmail.token-key:}") String configuredKey) {
        this.configuredKey = configuredKey;
    }

    private SecretKeySpec key() {
        // Read the encryption key from local configuration, never from the database.
        try {
            byte[] raw = Base64.getDecoder().decode(configuredKey);
            if (raw.length != 32) throw new IllegalArgumentException();
            return new SecretKeySpec(raw, "AES");
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("GMAIL_TOKEN_ENCRYPTION_KEY must be a base64-encoded 32-byte key");
        }
    }

    public void verifyConfigured() { key(); }

    public String encrypt(String plaintext) {
        try {
            // A fresh IV makes repeated encryptions of the same token different.
            byte[] iv = new byte[12];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key(), new GCMParameterSpec(128, iv));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) { throw new IllegalStateException("Could not encrypt Gmail token", e); }
    }

    public String decrypt(String ciphertext) {
        try {
            // The first 12 bytes are the IV; the remainder is the encrypted token.
            byte[] combined = Base64.getDecoder().decode(ciphertext);
            if (combined.length < 29) throw new IllegalArgumentException("Invalid token");
            byte[] iv = java.util.Arrays.copyOfRange(combined, 0, 12);
            byte[] encrypted = java.util.Arrays.copyOfRange(combined, 12, combined.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception e) { throw new IllegalStateException("Could not decrypt Gmail token", e); }
    }
}
