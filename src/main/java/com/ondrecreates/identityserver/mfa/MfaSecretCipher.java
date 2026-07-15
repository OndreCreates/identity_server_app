package com.ondrecreates.identityserver.mfa;

import com.ondrecreates.identityserver.crypto.AesGcmCipher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * AES-256-GCM encryption for TOTP secrets. Unlike passwords, a TOTP secret must be
 * readable again to compute the expected code, so it's encrypted rather than hashed.
 */
@Component
public class MfaSecretCipher {

    private final AesGcmCipher cipher;

    public MfaSecretCipher(@Value("${app.mfa.encryption-key}") String base64Key) {
        this.cipher = new AesGcmCipher(base64Key);
    }

    public byte[] encrypt(String plaintext) {
        return cipher.encrypt(plaintext.getBytes(StandardCharsets.UTF_8));
    }

    public String decrypt(byte[] ivAndCiphertext) {
        return new String(cipher.decrypt(ivAndCiphertext), StandardCharsets.UTF_8);
    }
}
