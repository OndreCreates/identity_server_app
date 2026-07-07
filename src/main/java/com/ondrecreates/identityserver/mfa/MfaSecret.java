package com.ondrecreates.identityserver.mfa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "mfa_secret")
public class MfaSecret {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "secret_encrypted", nullable = false)
    private byte[] secretEncrypted;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected MfaSecret() {
        // for JPA
    }

    public MfaSecret(Long userId, byte[] secretEncrypted) {
        this.userId = userId;
        this.secretEncrypted = secretEncrypted;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public byte[] getSecretEncrypted() {
        return secretEncrypted;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
