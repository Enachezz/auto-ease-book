-- Bootstrap system admin. Change this password in production.
-- Default login: admin@auto-ease.local / password (BCrypt matches Spring Security reference hash for "password").

INSERT INTO app_user (uuid, created_date, modified_date, email, phone, type, password)
VALUES (
    'a0000000-0000-4000-8000-000000000001',
    NOW(),
    NOW(),
    'admin@auto-ease.local',
    NULL,
    'ADMIN',
    '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG'
)
ON CONFLICT (uuid) DO NOTHING;

INSERT INTO profiles (id, user_id, full_name, email, phone, avatar_url, created_date, modified_date)
SELECT 'b0000000-0000-4000-8000-000000000001'::uuid,
       u.uuid,
       'System Admin',
       u.email,
       NULL,
       NULL,
       NOW(),
       NOW()
FROM app_user u
WHERE u.uuid = 'a0000000-0000-4000-8000-000000000001'
  AND NOT EXISTS (SELECT 1 FROM profiles p WHERE p.user_id = u.uuid);
