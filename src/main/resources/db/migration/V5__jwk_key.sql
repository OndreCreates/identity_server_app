-- Persists the RSA key pair used to sign JWTs, so a server restart no longer invalidates
-- every previously-issued token. The private key is encrypted (not hashed) since it has to
-- be read back to sign new tokens; the public key doesn't need protecting -- it's served as-is
-- from /oauth2/jwks.
CREATE TABLE jwk_key (
    id bigint AUTO_INCREMENT PRIMARY KEY,
    key_id varchar(64) NOT NULL,
    public_key_der varbinary(1024) NOT NULL,
    private_key_der_encrypted varbinary(4096) NOT NULL,
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
);
