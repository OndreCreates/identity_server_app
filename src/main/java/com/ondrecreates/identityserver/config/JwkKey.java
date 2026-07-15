package com.ondrecreates.identityserver.config;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "jwk_key")
public class JwkKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "key_id", nullable = false, length = 64)
    private String keyId;

    @Column(name = "public_key_der", nullable = false)
    private byte[] publicKeyDer;

    @Column(name = "private_key_der_encrypted", nullable = false)
    private byte[] privateKeyDerEncrypted;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected JwkKey() {
        // for JPA
    }

    public JwkKey(String keyId, byte[] publicKeyDer, byte[] privateKeyDerEncrypted) {
        this.keyId = keyId;
        this.publicKeyDer = publicKeyDer;
        this.privateKeyDerEncrypted = privateKeyDerEncrypted;
    }

    public Long getId() {
        return id;
    }

    public String getKeyId() {
        return keyId;
    }

    public byte[] getPublicKeyDer() {
        return publicKeyDer;
    }

    public byte[] getPrivateKeyDerEncrypted() {
        return privateKeyDerEncrypted;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
