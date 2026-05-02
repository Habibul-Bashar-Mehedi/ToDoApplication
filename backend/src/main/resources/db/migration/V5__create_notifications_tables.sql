-- V5: Create notifications and notification_preferences tables
-- notifications: in-app messages delivered to users about task/module events.
-- notification_preferences: per-user toggles controlling which reminder types are sent.

CREATE TABLE notifications (
    id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id    BIGINT UNSIGNED NOT NULL,
    task_id    BIGINT UNSIGNED NULL,
    type       ENUM(
                   'REMINDER_15MIN',
                   'REMINDER_1HOUR',
                   'REMINDER_1DAY',
                   'REMINDER_2DAYS',
                   'OVERDUE',
                   'INVITATION',
                   'SYSTEM'
               )               NOT NULL,
    message    VARCHAR(500)    NOT NULL,
    is_read    TINYINT(1)      NOT NULL DEFAULT 0,
    created_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_notifications      PRIMARY KEY (id),
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users (id),
    -- task_id is nullable: INVITATION and SYSTEM notifications may not reference a task
    CONSTRAINT fk_notifications_task FOREIGN KEY (task_id) REFERENCES tasks (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Index for fetching all notifications for a user (notification centre)
CREATE INDEX idx_notifications_user_id    ON notifications (user_id);

-- Index for the cleanup scheduler (delete notifications older than 30 days)
CREATE INDEX idx_notifications_created_at ON notifications (created_at);

-- Index for unread count queries (Property 26, 27)
CREATE INDEX idx_notifications_is_read    ON notifications (is_read);

-- ---------------------------------------------------------------------------

CREATE TABLE notification_preferences (
    id                    BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id               BIGINT UNSIGNED NOT NULL,
    reminder_15min_enabled TINYINT(1)     NOT NULL DEFAULT 1,
    reminder_1hour_enabled TINYINT(1)     NOT NULL DEFAULT 1,
    reminder_1day_enabled  TINYINT(1)     NOT NULL DEFAULT 1,
    reminder_2days_enabled TINYINT(1)     NOT NULL DEFAULT 1,
    overdue_enabled        TINYINT(1)     NOT NULL DEFAULT 1,

    CONSTRAINT pk_notification_preferences      PRIMARY KEY (id),
    CONSTRAINT fk_notification_preferences_user FOREIGN KEY (user_id) REFERENCES users (id),
    -- One preferences record per user (OneToOne relationship)
    CONSTRAINT uq_notification_preferences_user UNIQUE (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
