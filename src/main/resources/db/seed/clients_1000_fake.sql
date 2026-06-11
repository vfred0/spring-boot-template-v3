-- Seed script: generates 1000 fictitious clients and matching zero-balance accounts.
-- Safe to re-run: duplicate phones and accounts are ignored.

WITH generated_clients AS (
    SELECT
        CONCAT('Client', LPAD(gs::text, 4, '0')) AS first_name,
        CONCAT('Surname', LPAD(gs::text, 4, '0')) AS last_name,
        CONCAT('+3707', LPAD(gs::text, 7, '0')) AS phone
    FROM generate_series(1, 1000) AS gs
),
inserted_clients AS (
    INSERT INTO client (first_name, last_name, phone)
    SELECT gc.first_name, gc.last_name, gc.phone
    FROM generated_clients gc
    ON CONFLICT (phone) DO NOTHING
    RETURNING id
)
INSERT INTO account (balance, client_id, version)
SELECT 0, ic.id, 0
FROM inserted_clients ic
ON CONFLICT (client_id) DO NOTHING;

