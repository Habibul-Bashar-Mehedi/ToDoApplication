-- V6: Add last_edited_by to tasks table
-- Tracks which user last modified a task (update, complete/revert, cancel).
-- NULL means the task has never been edited after creation.

ALTER TABLE tasks
    ADD COLUMN last_edited_by BIGINT UNSIGNED NULL AFTER created_by,
    ADD CONSTRAINT fk_tasks_last_editor FOREIGN KEY (last_edited_by) REFERENCES users (id);

CREATE INDEX idx_tasks_last_edited_by ON tasks (last_edited_by);
