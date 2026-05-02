package com.todoapp.entity;

import java.io.Serializable;
import java.util.Objects;

/**
 * Composite primary key for {@link TaskCompletion}.
 */
public class TaskCompletionId implements Serializable {

    private Long task;
    private Long user;

    public TaskCompletionId() {}

    public TaskCompletionId(Long task, Long user) {
        this.task = task;
        this.user = user;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TaskCompletionId)) return false;
        TaskCompletionId that = (TaskCompletionId) o;
        return Objects.equals(task, that.task) && Objects.equals(user, that.user);
    }

    @Override
    public int hashCode() {
        return Objects.hash(task, user);
    }
}
