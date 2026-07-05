-- Seed data (dev only): admin user "admin@identity-server.dev" / password "admin123"
INSERT INTO app_user (email, password, enabled)
VALUES ('admin@identity-server.dev', '$2b$10$OXbJYXsyjWnCkyV6QHVcY.nxeRgRmvfx4hnNJb3J95Q.SJDvFi4Zu', TRUE);

INSERT INTO user_role (user_id, role)
SELECT id, 'ADMIN' FROM app_user WHERE email = 'admin@identity-server.dev';
