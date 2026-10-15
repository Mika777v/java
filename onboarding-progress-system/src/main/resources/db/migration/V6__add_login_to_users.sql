-- Login: password (BCrypt hash; NULL = cannot log in) and role.
ALTER TABLE users ADD COLUMN password_hash VARCHAR(100);
ALTER TABLE users ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'MEMBER'
    CHECK (role IN ('ADMIN', 'MEMBER'));
