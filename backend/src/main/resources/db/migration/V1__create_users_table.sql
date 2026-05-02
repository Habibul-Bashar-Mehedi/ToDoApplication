-- V1: Create users table
-- Stores registered user accounts with authentication and verification state.

CREATE TABLE users (
    id                              BIGINT UNSIGNED     NOT NULL AUTO_INCREMENT,
    email                           VARCHAR(255)        NOT NULL,
    password_hash                   VARCHAR(255)        NOT NULL,
    display_name                    VARCHAR(100)        NOT NULL,
    email_verified                  TINYINT(1)          NOT NULL DEFAULT 0,
    email_verification_token        VARCHAR(255)        NULL,
    email_verification_expires_at   DATETIME            NULL,
    failed_login_attempts           INT                 NOT NULL DEFAULT 0,
    locked_at                       DATETIME            NULL,
    password_reset_token            VARCHAR(255)        NULL,
    password_reset_expires_at       DATETIME            NULL,
    created_at                      DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                      DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at                      DATETIME            NULL,

    CONSTRAINT pk_users PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Unique index on email for fast lookup and uniqueness enforcement
CREATE UNIQUE INDEX idx_users_email
    ON users (email);

-- Index for email verification token lookups (verify-email endpoint)
CREATE INDEX idx_users_verification_token
    ON users (email_verification_token);

-- Index for password reset token lookups (reset-password endpoint)
CREATE INDEX idx_users_reset_token
    ON users (password_reset_token);
