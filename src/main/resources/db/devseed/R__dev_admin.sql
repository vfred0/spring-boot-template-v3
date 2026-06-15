-- DEV ONLY — auto-applied by Flyway only when classpath:db/devseed is on the
-- migration path (set via SPRING_FLYWAY_LOCATIONS in docker-compose.yaml).
-- Production keeps the default location (classpath:db/migration) and never runs this.
--
-- Provisions a working ADMIN for ALL security modes out of the box:
--   KEYCLOAK_JWT / KEYCLOAK_OPAQUE -> realm user 'admin' (admin/admin); its
--                                     Keycloak id is pinned in realm-export.json,
--                                     so binding that sub is deterministic.
--   API_KEY                        -> subject 'demo-admin' + raw key
--                                     sk_live_demo_admin_key_AAAA (public hash).
--
-- KNOWN credentials — rotate or remove before any real deployment.
-- Repeatable (R__) and idempotent: re-runs safely on every startup.

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

INSERT INTO identity.user_roles (keycloak_sub, role_id)
SELECT 'aaaaaaaa-aaaa-4aaa-aaaa-aaaaaaaaaaaa', id FROM identity.roles WHERE name = 'ADMIN'
ON CONFLICT (keycloak_sub) DO UPDATE SET role_id = EXCLUDED.role_id;

INSERT INTO identity.user_roles (keycloak_sub, role_id)
SELECT 'demo-admin', id FROM identity.roles WHERE name = 'ADMIN'
ON CONFLICT (keycloak_sub) DO UPDATE SET role_id = EXCLUDED.role_id;

INSERT INTO security.api_keys (key_hash, prefix_hint, subject, label, created_at)
VALUES ('ea41c12893ddefbf93770df12baa08d1f0397ae6fa3e041cb248ea29b9384d7d',
        'sk_live_...AAAA', 'demo-admin', 'demo admin key', now())
ON CONFLICT (key_hash) DO NOTHING;
