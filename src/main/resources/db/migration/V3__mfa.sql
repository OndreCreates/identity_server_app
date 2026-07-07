-- TOTP secret: encrypted (not hashed) because the server needs to read it back to verify codes.
CREATE TABLE mfa_secret (
    id bigint AUTO_INCREMENT PRIMARY KEY,
    user_id bigint NOT NULL,
    secret_encrypted varbinary(512) NOT NULL,
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_mfa_secret_user UNIQUE (user_id),
    CONSTRAINT fk_mfa_secret_user FOREIGN KEY (user_id) REFERENCES app_user (id) ON DELETE CASCADE
);

-- Recovery codes: hashed (not encrypted) -- unlike the TOTP secret, we only ever need to
-- compare them, never read the plaintext back.
CREATE TABLE mfa_recovery_code (
    id bigint AUTO_INCREMENT PRIMARY KEY,
    user_id bigint NOT NULL,
    code_hash varchar(255) NOT NULL,
    used_at timestamp NULL,
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_mfa_recovery_code_user FOREIGN KEY (user_id) REFERENCES app_user (id) ON DELETE CASCADE
);
