-- V2: Create modules table
-- Stores task modules (projects/categories) owned by a user.
-- Module name uniqueness per owner is enforced at the application layer
-- (checked before insert/update where deleted_at IS NULL).

CREATE TABLE modules (
    id          BIGINT UNSIGNED                 NOT NULL AUTO_INCREMENT,
    owner_id    BIGINT UNSIGNED                 NOT NULL,
    name        VARCHAR(100)                    NOT NULL,
    description TEXT                            NULL,
    visibility  ENUM('PRIVATE', 'SHARED')       NOT NULL DEFAULT 'PRIVATE',
    created_at  DATETIME                        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME                        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at  DATETIME                        NULL,

    CONSTRAINT pk_modules PRIMARY KEY (id),
    CONSTRAINT fk_modules_owner FOREIGN KEY (owner_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Index for querying all modules owned by a specific user
CREATE INDEX idx_modules_owner_id
    ON modules (owner_id);
