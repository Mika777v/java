-- Personal link for new employees (/join/{token}).
-- Only a SHA-256 hash of the token is stored, so the link itself cannot be read from the database.
ALTER TABLE users ADD COLUMN access_token_hash VARCHAR(64);
ALTER TABLE users ADD COLUMN access_token_expires_at TIMESTAMPTZ;
CREATE UNIQUE INDEX uk_users_access_token_hash ON users (access_token_hash);
