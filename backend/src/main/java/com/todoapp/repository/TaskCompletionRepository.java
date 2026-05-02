package com.todoapp.repository;

import com.todoapp.entity.TaskCompletion;
import com.todoapp.entity.TaskCompletionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface TaskCompletionRepository extends JpaRepository<TaskCompletion, TaskCompletionId> {

    /** Check if a specific user has completed a specific task. */
    boolean existsByTaskIdAndUserId(Long taskId, Long userId);

    /** Delete the completion record (revert). */
    void deleteByTaskIdAndUserId(Long taskId, Long userId);

    /**
     * Returns the set of task IDs (from the given list) that the user has completed.
     * Used to bulk-annotate task lists without N+1 queries.
     */
    @Query("SELECT tc.task.id FROM TaskCompletion tc WHERE tc.task.id IN :taskIds AND tc.user.id = :userId")
    Set<Long> findCompletedTaskIdsByUserIdAndTaskIdIn(
            @Param("userId") Long userId,
            @Param("taskIds") List<Long> taskIds);
}
