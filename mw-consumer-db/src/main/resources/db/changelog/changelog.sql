-- liquibase formatted sql

-- changeset id:initial_tables2
-- preconditions onFail:HALT

CREATE TABLE IF NOT EXISTS weather (
  id VARCHAR(36),
  now BIGINT,
  temperature DOUBLE PRECISION,
  wind DOUBLE PRECISION,
  url VARCHAR(1024),
  CONSTRAINT pk_weather PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS error (
  id VARCHAR(36) NOT NULL,
  details VARCHAR(4096),
  CONSTRAINT pk_error PRIMARY KEY (id)
);

