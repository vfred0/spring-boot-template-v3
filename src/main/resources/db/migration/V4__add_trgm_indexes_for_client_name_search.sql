DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'pg_trgm') THEN
        CREATE INDEX IF NOT EXISTS idx_client_first_name_trgm
            ON client
            USING gin (lower(first_name) gin_trgm_ops);

        CREATE INDEX IF NOT EXISTS idx_client_last_name_trgm
            ON client
            USING gin (lower(last_name) gin_trgm_ops);
    END IF;
END
$$;

