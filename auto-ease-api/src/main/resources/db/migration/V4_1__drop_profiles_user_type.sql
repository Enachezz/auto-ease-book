-- V4.1: Remove user_type from profiles — APP_USER.type is the single source of truth.
-- V4 fixed the CHECK constraint on this column; this migration removes it entirely.

ALTER TABLE profiles DROP COLUMN user_type;
