-- ADMIN capability — mode-independent (KEYCLOAK_JWT, KEYCLOAK_OPAQUE, API_KEY).
--
-- Creates the ADMIN role and grants the two management permissions that gate the
-- protected admin APIs:
--   rbac:manage     -> /api/rbac
--   apikeys:manage  -> /api/api-keys
--
-- Defines the admin CAPABILITY only; it binds NO subject. Bind an admin subject
-- afterwards with bind_admin.sql (Keycloak modes) or rbac_apikey_demo.sql (dev).
--
-- Idempotent and self-healing: re-running restores the ADMIN role and its grants
-- even if they were removed through the API.
--
--   docker compose exec -T spring-boot-template-db \
--     psql -U spring-boot-template -d spring-boot-template \
--     < src/main/resources/db/seed/rbac_admin_role.sql

INSERT INTO identity.roles (name, description)
VALUES ('ADMIN', 'Full system access')
ON CONFLICT (name) DO NOTHING;

INSERT INTO identity.permissions (resource, action)
VALUES ('rbac', 'manage'),
       ('apikeys', 'manage')
ON CONFLICT (resource, action) DO NOTHING;

INSERT INTO identity.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM identity.roles r
JOIN identity.permissions p ON (p.resource, p.action) IN (('rbac', 'manage'), ('apikeys', 'manage'))
WHERE r.name = 'ADMIN'
ON CONFLICT DO NOTHING;
