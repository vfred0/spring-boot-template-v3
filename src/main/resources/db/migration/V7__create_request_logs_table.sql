CREATE TABLE request_logs (
    id            BIGSERIAL    PRIMARY KEY,
    type          VARCHAR(255),
    request_value VARCHAR(255),
    response      TEXT,
    created_at    TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_request_logs_created_at ON request_logs (created_at);
