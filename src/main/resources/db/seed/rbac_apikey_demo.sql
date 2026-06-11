-- DEV / DEMO SEED — DO NOT RUN IN PRODUCTION.
-- Minimal bootstrap: ONE admin able to manage all RBAC via the API.
-- Everything else (roles, permissions, assignments, keys) is created through /api/rbac
-- and /api/api-keys — no further SQL needed.
--
--   demo-admin (role ADMIN)  raw key: sk_live_demo_admin_key_AAAA
--
-- Apply against the application DB, e.g.:
--   docker compose exec -T postgres-app psql -U app -d appdb < src/main/resources/db/seed/rbac_apikey_demo.sql
--
-- Self-contained: recreates the ADMIN role if it was deleted, so re-running this
-- always restores a working admin even after the role was removed via the API.

INSERT INTO roles (name, description) VALUES ('ADMIN', 'Full system access')
ON CONFLICT (name) DO NOTHING;

INSERT INTO permissions (resource, action) VALUES ('apikeys', 'manage')
ON CONFLICT (resource, action) DO NOTHING;

INSERT INTO permissions (resource, action) VALUES ('rbac', 'manage')
ON CONFLICT (resource, action) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.resource = 'apikeys' AND p.action = 'manage'
WHERE r.name = 'ADMIN'
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.resource = 'rbac' AND p.action = 'manage'
WHERE r.name = 'ADMIN'
ON CONFLICT DO NOTHING;

INSERT INTO user_roles (keycloak_sub, role_id)
SELECT 'demo-admin', id FROM roles WHERE name = 'ADMIN'
ON CONFLICT (keycloak_sub) DO NOTHING;

INSERT INTO api_keys (key_hash, prefix_hint, subject, label, created_at)
VALUES
    ('ea41c12893ddefbf93770df12baa08d1f0397ae6fa3e041cb248ea29b9384d7d',
     'sk_live_...AAAA', 'demo-admin', 'demo admin key', now())
ON CONFLICT (key_hash) DO NOTHING;
