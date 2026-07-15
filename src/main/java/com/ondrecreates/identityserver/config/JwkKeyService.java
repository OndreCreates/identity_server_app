package com.ondrecreates.identityserver.config;

import com.nimbusds.jose.jwk.RSAKey;
import com.ondrecreates.identityserver.crypto.AesGcmCipher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.UUID;

/**
 * Loads the RSA key used to sign JWTs from the database, generating and persisting one the
 * first time the app runs. Without this, every restart would generate a fresh key in memory
 * (the original approach here) and silently invalidate every token issued before it.
 */
@Service
public class JwkKeyService {

    private final JwkKeyRepository jwkKeyRepository;
    private final AesGcmCipher cipher;

    public JwkKeyService(JwkKeyRepository jwkKeyRepository, @Value("${app.jwk.encryption-key}") String encryptionKey) {
        this.jwkKeyRepository = jwkKeyRepository;
        this.cipher = new AesGcmCipher(encryptionKey);
    }

    @Transactional
    public RSAKey loadOrGenerate() {
        JwkKey stored = jwkKeyRepository.findFirstByOrderByCreatedAtDesc().orElseGet(this::generateAndPersist);
        return toRsaKey(stored);
    }

    private JwkKey generateAndPersist() {
        KeyPair keyPair = generateRsaKeyPair();
        byte[] publicKeyDer = keyPair.getPublic().getEncoded();
        byte[] privateKeyDerEncrypted = cipher.encrypt(keyPair.getPrivate().getEncoded());
        JwkKey jwkKey = new JwkKey(UUID.randomUUID().toString(), publicKeyDer, privateKeyDerEncrypted);
        return jwkKeyRepository.save(jwkKey);
    }

    private RSAKey toRsaKey(JwkKey stored) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            RSAPublicKey publicKey = (RSAPublicKey) keyFactory
                    .generatePublic(new X509EncodedKeySpec(stored.getPublicKeyDer()));
            byte[] privateKeyDer = cipher.decrypt(stored.getPrivateKeyDerEncrypted());
            RSAPrivateKey privateKey = (RSAPrivateKey) keyFactory
                    .generatePrivate(new PKCS8EncodedKeySpec(privateKeyDer));

            return new RSAKey.Builder(publicKey)
                    .privateKey(privateKey)
                    .keyID(stored.getKeyId())
                    .build();
        } catch (InvalidKeySpecException | NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Failed to reconstruct the stored JWK signing key", ex);
        }
    }

    private static KeyPair generateRsaKeyPair() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            return keyPairGenerator.generateKeyPair();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Failed to generate RSA key pair for JWT signing", ex);
        }
    }
}
