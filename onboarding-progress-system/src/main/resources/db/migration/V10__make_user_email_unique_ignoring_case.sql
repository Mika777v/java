-- The app treats e-mail addresses without regard to case (Taro@example.com = taro@example.com),
-- but the UNIQUE constraint of V3 does. This index makes the database reject such duplicates too,
-- even when two registrations arrive at the same time.
CREATE UNIQUE INDEX uk_users_email_lower ON users (lower(email));
