-- true when an administrator set this user's password: the user must change it at the first login.
ALTER TABLE users ADD COLUMN password_change_required BOOLEAN NOT NULL DEFAULT false;
