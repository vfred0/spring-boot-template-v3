CREATE SCHEMA IF NOT EXISTS scheduling;

-- Official db-scheduler 16.12.0 Postgres DDL (db-scheduler/src/test/resources/postgresql_tables.sql),
-- relocated into the scheduling schema.
CREATE TABLE scheduling.scheduled_tasks (
    task_name            TEXT        NOT NULL,
    task_instance        TEXT        NOT NULL,
    task_data            BYTEA,
    execution_time       TIMESTAMPTZ NOT NULL,
    picked               BOOLEAN     NOT NULL,
    picked_by            TEXT,
    last_success         TIMESTAMPTZ,
    last_failure         TIMESTAMPTZ,
    consecutive_failures INT,
    last_heartbeat       TIMESTAMPTZ,
    version              BIGINT      NOT NULL,
    priority             SMALLINT,
    PRIMARY KEY (task_name, task_instance)
);

CREATE INDEX execution_time_idx ON scheduling.scheduled_tasks (execution_time);
CREATE INDEX last_heartbeat_idx ON scheduling.scheduled_tasks (last_heartbeat);
CREATE INDEX priority_execution_time_idx ON scheduling.scheduled_tasks (priority DESC, execution_time ASC);
