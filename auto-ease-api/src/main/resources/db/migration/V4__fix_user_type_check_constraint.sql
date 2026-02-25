-- V4: Fix profiles.user_type CHECK constraint to match Java enum names (uppercase).
-- V2 created it with lowercase values, but @Enumerated(EnumType.STRING) stores uppercase.

ALTER TABLE profiles DROP CONSTRAINT chk_profiles_user_type;
ALTER TABLE profiles ALTER COLUMN user_type SET DEFAULT 'CAR_OWNER';
ALTER TABLE profiles ADD CONSTRAINT chk_profiles_user_type
    CHECK (user_type IN ('CAR_OWNER', 'GARAGE', 'ADMIN'));
