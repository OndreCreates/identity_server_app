-- email is stored separately from user_id (not just via the FK) because a failed login
-- attempt against a nonexistent address has no app_user row to join to, and we still want
-- to know which address was targeted.
CREATE TABLE audit_log (
    id bigint AUTO_INCREMENT PRIMARY KEY,
    user_id bigint NULL,
    email varchar(255) NOT NULL,
    event_type varchar(50) NOT NULL,
    ip_address varchar(45) NULL,
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_log_user FOREIGN KEY (user_id) REFERENCES app_user (id) ON DELETE SET NULL
);

CREATE INDEX idx_audit_log_email ON audit_log (email);
CREATE INDEX idx_audit_log_created_at ON audit_log (created_at);
