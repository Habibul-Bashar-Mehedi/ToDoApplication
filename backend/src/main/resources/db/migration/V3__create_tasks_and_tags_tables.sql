-- V3: Create tasks, tags, and task_tags tables
-- tasks: individual work items belonging to a module, created by a user.
-- tags: global tag vocabulary (unique by name).
-- task_tags: many-to-many join between tasks and tags.

CREATE TABLE tasks (
    id                  BIGINT UNSIGNED                         NOT NULL AUTO_INCREMENT,
    module_id           BIGINT UNSIGNED                         NOT NULL,
    created_by          BIGINT UNSIGNED                         NOT NULL,
    title               VARCHAR(200)                            NOT NULL,
    description         TEXT                                    NULL,
    status              ENUM('PENDING', 'COMPLETED', 'CANCELLED') NOT NULL DEFAULT 'PENDING',
    priority            ENUM('HIGH', 'MEDIUM', 'LOW')           NOT NULL DEFAULT 'MEDIUM',
    due_date            DATETIME                                NULL,

    -- Reminder toggle flags (user-selected intervals)
    reminder_15min      TINYINT(1)                              NOT NULL DEFAULT 0,
    reminder_1hour      TINYINT(1)                              NOT NULL DEFAULT 0,
    reminder_1day       TINYINT(1)                              NOT NULL DEFAULT 0,
    reminder_2days      TINYINT(1)                              NOT NULL DEFAULT 0,

    -- Reminder sent flags (set by scheduler to prevent duplicate notifications)
    reminder_15min_sent TINYINT(1)                              NOT NULL DEFAULT 0,
    reminder_1hour_sent TINYINT(1)                              NOT NULL DEFAULT 0,
    reminder_1day_sent  TINYINT(1)                              NOT NULL DEFAULT 0,
    reminder_2days_sent TINYINT(1)                              NOT NULL DEFAULT 0,

    -- Overdue notification idempotency flag (Property 23: sent exactly once)
    overdue_notified    TINYINT(1)                              NOT NULL DEFAULT 0,

    completed_at        DATETIME                                NULL,
    cancelled_at        DATETIME                                NULL,
    cancellation_reason VARCHAR(500)                            NULL,
    created_at          DATETIME                                NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME                                NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at          DATETIME                                NULL,

    CONSTRAINT pk_tasks PRIMARY KEY (id),
    CONSTRAINT fk_tasks_module  FOREIGN KEY (module_id)  REFERENCES modules (id),
    CONSTRAINT fk_tasks_creator FOREIGN KEY (created_by) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Indexes for common query patterns
CREATE INDEX idx_tasks_module_id  ON tasks (module_id);
CREATE INDEX idx_tasks_created_by ON tasks (created_by);
CREATE INDEX idx_tasks_status     ON tasks (status);
CREATE INDEX idx_tasks_due_date   ON tasks (due_date);
CREATE INDEX idx_tasks_created_at ON tasks (created_at);

-- ---------------------------------------------------------------------------

CREATE TABLE tags (
    id   BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    name VARCHAR(50)     NOT NULL,

    CONSTRAINT pk_tags    PRIMARY KEY (id),
    CONSTRAINT uq_tag_name UNIQUE (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------

CREATE TABLE task_tags (
    task_id BIGINT UNSIGNED NOT NULL,
    tag_id  BIGINT UNSIGNED NOT NULL,

    CONSTRAINT pk_task_tags      PRIMARY KEY (task_id, tag_id),
    CONSTRAINT fk_task_tags_task FOREIGN KEY (task_id) REFERENCES tasks (id) ON DELETE CASCADE,
    CONSTRAINT fk_task_tags_tag  FOREIGN KEY (tag_id)  REFERENCES tags  (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
