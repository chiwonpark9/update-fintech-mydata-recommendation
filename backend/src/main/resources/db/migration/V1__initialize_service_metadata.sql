CREATE TABLE service_metadata
(
    metadata_key   VARCHAR(100) NOT NULL,
    metadata_value VARCHAR(255) NOT NULL,
    created_at     TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_service_metadata PRIMARY KEY (metadata_key)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

INSERT INTO service_metadata (metadata_key, metadata_value)
VALUES ('schema_initialized_by', 'flyway');
