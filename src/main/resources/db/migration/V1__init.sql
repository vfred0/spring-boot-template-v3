CREATE SCHEMA IF NOT EXISTS core;
CREATE SCHEMA IF NOT EXISTS identity;
CREATE SCHEMA IF NOT EXISTS security;

-- -------------------------------------------------------------------------
-- core
-- -------------------------------------------------------------------------

CREATE TABLE core.clients (
    id         BIGSERIAL    PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name  VARCHAR(100) NOT NULL,
    phone      VARCHAR(50)  NOT NULL UNIQUE
);

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'pg_trgm') THEN
        CREATE INDEX IF NOT EXISTS idx_clients_first_name_trgm
            ON core.clients USING gin (lower(first_name) gin_trgm_ops);
        CREATE INDEX IF NOT EXISTS idx_clients_last_name_trgm
            ON core.clients USING gin (lower(last_name) gin_trgm_ops);
    END IF;
END
$$;

CREATE TABLE core.accounts (
    id        BIGSERIAL      PRIMARY KEY,
    balance   NUMERIC(19, 2) NOT NULL DEFAULT 0,
    client_id BIGINT         NOT NULL,
    version   BIGINT         NOT NULL DEFAULT 0,
    CONSTRAINT fk_accounts_client FOREIGN KEY (client_id) REFERENCES core.clients (id),
    CONSTRAINT uq_accounts_client UNIQUE (client_id)
);

CREATE INDEX idx_accounts_client_id ON core.accounts (client_id);

CREATE TABLE core.requests (
    id                UUID        PRIMARY KEY,
    type              VARCHAR(50) NOT NULL,
    status            VARCHAR(50) NOT NULL,
    created_at        TIMESTAMPTZ NOT NULL,
    status_changed_at TIMESTAMPTZ NOT NULL,
    request_data      JSONB       NOT NULL,
    response_data     JSONB,
    CONSTRAINT chk_requests_type   CHECK (type   IN ('CLIENT_CREATE', 'OTHER')),
    CONSTRAINT chk_requests_status CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED'))
);

CREATE INDEX idx_requests_type       ON core.requests (type);
CREATE INDEX idx_requests_status     ON core.requests (status);
CREATE INDEX idx_requests_created_at ON core.requests (created_at);

CREATE TABLE core.request_logs (
    id            BIGSERIAL   PRIMARY KEY,
    type          VARCHAR(255),
    request_value VARCHAR(255),
    response      TEXT,
    created_at    TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_request_logs_created_at ON core.request_logs (created_at);

-- -------------------------------------------------------------------------
-- identity
-- -------------------------------------------------------------------------

CREATE TABLE identity.roles (
    id          BIGSERIAL    PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255)
);

CREATE TABLE identity.permissions (
    id       BIGSERIAL    PRIMARY KEY,
    resource VARCHAR(100) NOT NULL,
    action   VARCHAR(50)  NOT NULL,
    CONSTRAINT uq_permissions UNIQUE (resource, action)
);

CREATE TABLE identity.user_roles (
    id           BIGSERIAL    PRIMARY KEY,
    keycloak_sub VARCHAR(255) NOT NULL UNIQUE,
    role_id      BIGINT       NOT NULL REFERENCES identity.roles (id) ON DELETE CASCADE
);

CREATE TABLE identity.role_permissions (
    role_id       BIGINT NOT NULL REFERENCES identity.roles (id)       ON DELETE CASCADE,
    permission_id BIGINT NOT NULL REFERENCES identity.permissions (id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

INSERT INTO identity.roles (name, description) VALUES
    ('ADMIN',        'Full system access'),
    ('DATA_ANALYST', 'Read and update customer data');

INSERT INTO identity.permissions (resource, action) VALUES
    ('customers', 'read'),
    ('customers', 'update'),
    ('customers', 'delete'),
    ('reports',   'read'),
    ('reports',   'create');

INSERT INTO identity.role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM identity.roles r
CROSS JOIN identity.permissions p
WHERE r.name = 'ADMIN';

INSERT INTO identity.role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM identity.roles r
JOIN identity.permissions p ON p.resource = 'customers' AND p.action IN ('read', 'update')
WHERE r.name = 'DATA_ANALYST';

-- -------------------------------------------------------------------------
-- security
-- -------------------------------------------------------------------------

CREATE TABLE security.api_keys (
    id           BIGSERIAL    PRIMARY KEY,
    key_hash     VARCHAR(64)  NOT NULL UNIQUE,
    prefix_hint  VARCHAR(32)  NOT NULL,
    subject      VARCHAR(255) NOT NULL,
    label        VARCHAR(100) NOT NULL,
    allowed_ips  VARCHAR(512),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    expires_at   TIMESTAMPTZ,
    revoked_at   TIMESTAMPTZ,
    last_used_at TIMESTAMPTZ,
    last_used_ip VARCHAR(45)
);

CREATE INDEX idx_api_keys_subject ON security.api_keys (subject);

CREATE TABLE security.auth_audits (
    id             BIGSERIAL    PRIMARY KEY,
    occurred_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    subject        VARCHAR(255),
    api_key_id     BIGINT,
    client_ip      VARCHAR(45),
    user_agent     VARCHAR(512),
    outcome        VARCHAR(32)  NOT NULL,
    request_path   VARCHAR(512),
    request_method VARCHAR(16)
);

CREATE INDEX idx_auth_audits_occurred_at ON security.auth_audits (occurred_at);
CREATE INDEX idx_auth_audits_subject     ON security.auth_audits (subject);

