-- V7: Per-user task completion tracking
-- Each row means "user_id has personally marked task_id as done".
-- The shared tasks.status column is now only used for CANCELLED state.
-- PENDING means nobody has completed it yet (or it was reverted by everyone).

CREATE TABLE task_completions (
    task_id      BIGINT UNSIGNED NOT NULL,
    user_id      BIGINT UNSIGNED NOT NULL,
    completed_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_task_completions       PRIMARY KEY (task_id, user_id),
    CONSTRAINT fk_tc_task FOREIGN KEY (task_id) REFERENCES tasks (id) ON DELETE CASCADE,
    CONSTRAINT fk_tc_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_task_completions_user ON task_completions (user_id);
