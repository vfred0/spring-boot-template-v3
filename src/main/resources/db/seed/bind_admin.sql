-- Bind one subject to the ADMIN role.
-- Run rbac_admin_role.sql first (it creates the ADMIN role and its permissions).
--
-- The subject is whatever the active security mode authenticates as:
--   KEYCLOAK_JWT / KEYCLOAK_OPAQUE -> the Keycloak subject, returned as
--                                     `data.username` by GET /api/me
--   API_KEY                        -> the api_keys.subject string
--
-- Authoritative: forces the subject to ADMIN even if it already had another role.
-- Authorities are read from the DB on every request, so the grant takes effect
-- immediately — no re-login.
--
--   docker compose exec -T spring-boot-template-db \
--     psql -U spring-boot-template -d spring-boot-template \
--     -v subject=<paste-subject-here> \
--     < src/main/resources/db/seed/bind_admin.sql

INSERT INTO identity.user_roles (keycloak_sub, role_id)
SELECT :'subject', id FROM identity.roles WHERE name = 'ADMIN'
ON CONFLICT (keycloak_sub) DO UPDATE SET role_id = EXCLUDED.role_id;
