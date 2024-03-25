-- liquibase formatted sql

-- changeset id:initial_tables2
-- preconditions onFail:HALT

CREATE TABLE IF NOT EXISTS conversion (
  id VARCHAR(36),
  source_id BIGINT NOT NULL,
  base VARCHAR(4),
  convert VARCHAR(4),
  base_amount DECIMAL(12,4),
  conversion_amount DECIMAL(12,4),
  CONSTRAINT pk_conversion PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS error (
  id VARCHAR(36) NOT NULL,
  details VARCHAR(4096),
  CONSTRAINT pk_error PRIMARY KEY (id)
);

