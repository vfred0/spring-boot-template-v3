CREATE TABLE roles (
    id          BIGSERIAL    PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255)
);

CREATE TABLE permissions (
    id       BIGSERIAL   PRIMARY KEY,
    resource VARCHAR(100) NOT NULL,
    action   VARCHAR(50)  NOT NULL,
    CONSTRAINT uq_permissions UNIQUE (resource, action)
);

CREATE TABLE user_roles (
    id           BIGSERIAL    PRIMARY KEY,
    keycloak_sub VARCHAR(255) NOT NULL UNIQUE,
    role_id      BIGINT       NOT NULL REFERENCES roles (id) ON DELETE CASCADE
);

CREATE TABLE role_permissions (
    role_id       BIGINT NOT NULL REFERENCES roles (id) ON DELETE CASCADE,
    permission_id BIGINT NOT NULL REFERENCES permissions (id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

INSERT INTO roles (name, description) VALUES
    ('ADMIN',         'Full system access'),
    ('DATA_ANALYST',  'Read and update customer data');

INSERT INTO permissions (resource, action) VALUES
    ('customers', 'read'),
    ('customers', 'update'),
    ('customers', 'delete'),
    ('reports',   'read'),
    ('reports',   'create');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p WHERE r.name = 'ADMIN';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.resource = 'customers' AND p.action IN ('read', 'update')
WHERE r.name = 'DATA_ANALYST';
