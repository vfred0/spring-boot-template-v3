CREATE TABLE api_keys (
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

CREATE INDEX idx_api_keys_subject ON api_keys (subject);

CREATE TABLE auth_audit (
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

CREATE INDEX idx_auth_audit_occurred_at ON auth_audit (occurred_at);
CREATE INDEX idx_auth_audit_subject ON auth_audit (subject);
