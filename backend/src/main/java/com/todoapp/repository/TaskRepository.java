package com.todoapp.repository;

import com.todoapp.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByModuleIdAndDeletedAtIsNull(Long moduleId);

    Optional<Task> findByIdAndDeletedAtIsNull(Long id);

    /**
     * Find tasks due for a specific reminder interval.
     * Returns PENDING tasks where the due date falls within the given window,
     * the reminder toggle is enabled, the sent flag is false, and the task is not deleted.
     */
    @Query("""
            SELECT t FROM Task t
            WHERE t.status = 'PENDING'
              AND t.dueDate BETWEEN :windowStart AND :windowEnd
              AND t.deletedAt IS NULL
            """)
    List<Task> findTasksDueInWindow(@Param("windowStart") LocalDateTime windowStart,
                                    @Param("windowEnd") LocalDateTime windowEnd);

    /**
     * Find overdue tasks that have not yet been notified.
     * Returns PENDING tasks whose due date is in the past and overdue_notified is false.
     */
    @Query("""
            SELECT t FROM Task t
            WHERE t.status = 'PENDING'
              AND t.dueDate < :now
              AND t.overdueNotified = false
              AND t.deletedAt IS NULL
            """)
    List<Task> findOverdueTasks(@Param("now") LocalDateTime now);

    List<Task> findByModuleIdInAndDeletedAtIsNull(List<Long> moduleIds);

    // =========================================================================
    // Analytics queries
    // =========================================================================

    /**
     * Count tasks in accessible modules created within the date range.
     */
    @Query(value = """
            SELECT COUNT(t.id) FROM tasks t
            JOIN modules m ON t.module_id = m.id
            LEFT JOIN module_members mm ON m.id = mm.module_id AND mm.user_id = :userId
            WHERE (m.owner_id = :userId OR mm.user_id = :userId)
              AND t.created_at BETWEEN :from AND :to
              AND t.deleted_at IS NULL
            """, nativeQuery = true)
    long countTasksInRange(@Param("userId") Long userId,
                           @Param("from") LocalDateTime from,
                           @Param("to") LocalDateTime to);

    /**
     * Count tasks by status in accessible modules created within the date range.
     * Returns rows of [status, count].
     */
    @Query(value = """
            SELECT t.status, COUNT(t.id) as cnt FROM tasks t
            JOIN modules m ON t.module_id = m.id
            LEFT JOIN module_members mm ON m.id = mm.module_id AND mm.user_id = :userId
            WHERE (m.owner_id = :userId OR mm.user_id = :userId)
              AND t.created_at BETWEEN :from AND :to
              AND t.deleted_at IS NULL
            GROUP BY t.status
            """, nativeQuery = true)
    List<Object[]> countTasksByStatus(@Param("userId") Long userId,
                                      @Param("from") LocalDateTime from,
                                      @Param("to") LocalDateTime to);

    /**
     * Count overdue tasks (PENDING with due_date < now) in accessible modules.
     */
    @Query(value = """
            SELECT COUNT(t.id) FROM tasks t
            JOIN modules m ON t.module_id = m.id
            LEFT JOIN module_members mm ON m.id = mm.module_id AND mm.user_id = :userId
            WHERE (m.owner_id = :userId OR mm.user_id = :userId)
              AND t.status = 'PENDING'
              AND t.due_date < :now
              AND t.deleted_at IS NULL
            """, nativeQuery = true)
    long countOverdueTasks(@Param("userId") Long userId,
                           @Param("now") LocalDateTime now);

    /**
     * Count modules with at least one PENDING task accessible to the user.
     */
    @Query(value = """
            SELECT COUNT(DISTINCT m.id) FROM modules m
            LEFT JOIN module_members mm ON m.id = mm.module_id AND mm.user_id = :userId
            JOIN tasks t ON t.module_id = m.id
            WHERE (m.owner_id = :userId OR mm.user_id = :userId)
              AND t.status = 'PENDING'
              AND t.deleted_at IS NULL
              AND m.deleted_at IS NULL
            """, nativeQuery = true)
    long countActiveModules(@Param("userId") Long userId);

    /**
     * Count completed tasks in accessible modules within the date range.
     */
    @Query(value = """
            SELECT COUNT(t.id) FROM tasks t
            JOIN modules m ON t.module_id = m.id
            LEFT JOIN module_members mm ON m.id = mm.module_id AND mm.user_id = :userId
            WHERE (m.owner_id = :userId OR mm.user_id = :userId)
              AND t.status = 'COMPLETED'
              AND t.created_at BETWEEN :from AND :to
              AND t.deleted_at IS NULL
            """, nativeQuery = true)
    long countCompletedTasksInRange(@Param("userId") Long userId,
                                    @Param("from") LocalDateTime from,
                                    @Param("to") LocalDateTime to);

    /**
     * Count cancelled tasks in accessible modules created within the date range.
     */
    @Query(value = """
            SELECT COUNT(t.id) FROM tasks t
            JOIN modules m ON t.module_id = m.id
            LEFT JOIN module_members mm ON m.id = mm.module_id AND mm.user_id = :userId
            WHERE (m.owner_id = :userId OR mm.user_id = :userId)
              AND t.status = 'CANCELLED'
              AND t.created_at BETWEEN :from AND :to
              AND t.deleted_at IS NULL
            """, nativeQuery = true)
    long countCancelledTasksInRange(@Param("userId") Long userId,
                                    @Param("from") LocalDateTime from,
                                    @Param("to") LocalDateTime to);

    /**
     * Daily completion trend: count of tasks created and completed per day.
     * Returns rows of [date_label, created_count, completed_count].
     */
    @Query(value = """
            SELECT DATE(t.created_at) as date_label,
                   COUNT(t.id) as created_count,
                   SUM(CASE WHEN t.status = 'COMPLETED' THEN 1 ELSE 0 END) as completed_count
            FROM tasks t
            JOIN modules m ON t.module_id = m.id
            LEFT JOIN module_members mm ON m.id = mm.module_id AND mm.user_id = :userId
            WHERE (m.owner_id = :userId OR mm.user_id = :userId)
              AND t.created_at BETWEEN :from AND :to
              AND t.deleted_at IS NULL
            GROUP BY DATE(t.created_at)
            ORDER BY date_label
            """, nativeQuery = true)
    List<Object[]> getDailyTrend(@Param("userId") Long userId,
                                 @Param("from") LocalDateTime from,
                                 @Param("to") LocalDateTime to);

    /**
     * Weekly completion trend: count of tasks created and completed per ISO week.
     * Returns rows of [week_label, created_count, completed_count].
     */
    @Query(value = """
            SELECT DATE(DATE_SUB(t.created_at, INTERVAL WEEKDAY(t.created_at) DAY)) as date_label,
                   COUNT(t.id) as created_count,
                   SUM(CASE WHEN t.status = 'COMPLETED' THEN 1 ELSE 0 END) as completed_count
            FROM tasks t
            JOIN modules m ON t.module_id = m.id
            LEFT JOIN module_members mm ON m.id = mm.module_id AND mm.user_id = :userId
            WHERE (m.owner_id = :userId OR mm.user_id = :userId)
              AND t.created_at BETWEEN :from AND :to
              AND t.deleted_at IS NULL
            GROUP BY date_label
            ORDER BY date_label
            """, nativeQuery = true)
    List<Object[]> getWeeklyTrend(@Param("userId") Long userId,
                                  @Param("from") LocalDateTime from,
                                  @Param("to") LocalDateTime to);

    /**
     * Monthly completion trend: count of tasks created and completed per month.
     * Returns rows of [month_label, created_count, completed_count].
     */
    @Query(value = """
            SELECT DATE_FORMAT(t.created_at, '%Y-%m') as date_label,
                   COUNT(t.id) as created_count,
                   SUM(CASE WHEN t.status = 'COMPLETED' THEN 1 ELSE 0 END) as completed_count
            FROM tasks t
            JOIN modules m ON t.module_id = m.id
            LEFT JOIN module_members mm ON m.id = mm.module_id AND mm.user_id = :userId
            WHERE (m.owner_id = :userId OR mm.user_id = :userId)
              AND t.created_at BETWEEN :from AND :to
              AND t.deleted_at IS NULL
            GROUP BY date_label
            ORDER BY date_label
            """, nativeQuery = true)
    List<Object[]> getMonthlyTrend(@Param("userId") Long userId,
                                   @Param("from") LocalDateTime from,
                                   @Param("to") LocalDateTime to);

    /**
     * Module performance: completion rate per accessible module.
     * Returns rows of [module_id, module_name, total_count, completed_count, cancelled_count].
     */
    @Query(value = """
            SELECT m.id as module_id, m.name as module_name,
                   COUNT(t.id) as total_count,
                   SUM(CASE WHEN t.status = 'COMPLETED' THEN 1 ELSE 0 END) as completed_count,
                   SUM(CASE WHEN t.status = 'CANCELLED' THEN 1 ELSE 0 END) as cancelled_count
            FROM modules m
            LEFT JOIN module_members mm ON m.id = mm.module_id AND mm.user_id = :userId
            LEFT JOIN tasks t ON t.module_id = m.id AND t.deleted_at IS NULL
            WHERE (m.owner_id = :userId OR mm.user_id = :userId)
              AND m.deleted_at IS NULL
            GROUP BY m.id, m.name
            """, nativeQuery = true)
    List<Object[]> getModulePerformance(@Param("userId") Long userId);

    /**
     * Priority analysis: count and completion rate per priority.
     * Returns rows of [priority, total_count, completed_count, cancelled_count].
     */
    @Query(value = """
            SELECT t.priority,
                   COUNT(t.id) as total_count,
                   SUM(CASE WHEN t.status = 'COMPLETED' THEN 1 ELSE 0 END) as completed_count,
                   SUM(CASE WHEN t.status = 'CANCELLED' THEN 1 ELSE 0 END) as cancelled_count
            FROM tasks t
            JOIN modules m ON t.module_id = m.id
            LEFT JOIN module_members mm ON m.id = mm.module_id AND mm.user_id = :userId
            WHERE (m.owner_id = :userId OR mm.user_id = :userId)
              AND t.created_at BETWEEN :from AND :to
              AND t.deleted_at IS NULL
            GROUP BY t.priority
            """, nativeQuery = true)
    List<Object[]> getPriorityAnalysis(@Param("userId") Long userId,
                                       @Param("from") LocalDateTime from,
                                       @Param("to") LocalDateTime to);

    /**
     * Average completion time in days for tasks completed within the date range.
     */
    @Query(value = """
            SELECT AVG(TIMESTAMPDIFF(SECOND, t.created_at, t.completed_at)) / 86400.0
            FROM tasks t
            JOIN modules m ON t.module_id = m.id
            LEFT JOIN module_members mm ON m.id = mm.module_id AND mm.user_id = :userId
            WHERE (m.owner_id = :userId OR mm.user_id = :userId)
              AND t.status = 'COMPLETED'
              AND t.completed_at BETWEEN :from AND :to
              AND t.deleted_at IS NULL
            """, nativeQuery = true)
    Double getAvgCompletionDays(@Param("userId") Long userId,
                                @Param("from") LocalDateTime from,
                                @Param("to") LocalDateTime to);

    /**
     * Daily overdue trend: count of tasks that became overdue per day.
     * Returns rows of [date_label, overdue_count].
     */
    @Query(value = """
            SELECT DATE(t.due_date) as date_label, COUNT(t.id) as overdue_count
            FROM tasks t
            JOIN modules m ON t.module_id = m.id
            LEFT JOIN module_members mm ON m.id = mm.module_id AND mm.user_id = :userId
            WHERE (m.owner_id = :userId OR mm.user_id = :userId)
              AND t.status = 'PENDING'
              AND t.due_date BETWEEN :from AND :to
              AND t.due_date < :now
              AND t.deleted_at IS NULL
            GROUP BY DATE(t.due_date)
            ORDER BY date_label
            """, nativeQuery = true)
    List<Object[]> getDailyOverdueTrend(@Param("userId") Long userId,
                                        @Param("from") LocalDateTime from,
                                        @Param("to") LocalDateTime to,
                                        @Param("now") LocalDateTime now);

    /**
     * Weekly overdue trend.
     * Returns rows of [date_label, overdue_count].
     */
    @Query(value = """
            SELECT DATE(DATE_SUB(t.due_date, INTERVAL WEEKDAY(t.due_date) DAY)) as date_label,
                   COUNT(t.id) as overdue_count
            FROM tasks t
            JOIN modules m ON t.module_id = m.id
            LEFT JOIN module_members mm ON m.id = mm.module_id AND mm.user_id = :userId
            WHERE (m.owner_id = :userId OR mm.user_id = :userId)
              AND t.status = 'PENDING'
              AND t.due_date BETWEEN :from AND :to
              AND t.due_date < :now
              AND t.deleted_at IS NULL
            GROUP BY date_label
            ORDER BY date_label
            """, nativeQuery = true)
    List<Object[]> getWeeklyOverdueTrend(@Param("userId") Long userId,
                                         @Param("from") LocalDateTime from,
                                         @Param("to") LocalDateTime to,
                                         @Param("now") LocalDateTime now);

    /**
     * Monthly overdue trend.
     * Returns rows of [date_label, overdue_count].
     */
    @Query(value = """
            SELECT DATE_FORMAT(t.due_date, '%Y-%m') as date_label,
                   COUNT(t.id) as overdue_count
            FROM tasks t
            JOIN modules m ON t.module_id = m.id
            LEFT JOIN module_members mm ON m.id = mm.module_id AND mm.user_id = :userId
            WHERE (m.owner_id = :userId OR mm.user_id = :userId)
              AND t.status = 'PENDING'
              AND t.due_date BETWEEN :from AND :to
              AND t.due_date < :now
              AND t.deleted_at IS NULL
            GROUP BY date_label
            ORDER BY date_label
            """, nativeQuery = true)
    List<Object[]> getMonthlyOverdueTrend(@Param("userId") Long userId,
                                          @Param("from") LocalDateTime from,
                                          @Param("to") LocalDateTime to,
                                          @Param("now") LocalDateTime now);

    /**
     * Most productive day of week: DAYNAME with highest completed count.
     * Returns rows of [day_name, count].
     */
    @Query(value = """
            SELECT DAYNAME(t.completed_at) as day_name, COUNT(t.id) as cnt
            FROM tasks t
            JOIN modules m ON t.module_id = m.id
            LEFT JOIN module_members mm ON m.id = mm.module_id AND mm.user_id = :userId
            WHERE (m.owner_id = :userId OR mm.user_id = :userId)
              AND t.status = 'COMPLETED'
              AND t.completed_at BETWEEN :from AND :to
              AND t.deleted_at IS NULL
            GROUP BY DAYNAME(t.completed_at)
            ORDER BY cnt DESC
            LIMIT 1
            """, nativeQuery = true)
    List<Object[]> getMostProductiveDay(@Param("userId") Long userId,
                                        @Param("from") LocalDateTime from,
                                        @Param("to") LocalDateTime to);

    /**
     * Peak completion hour: HOUR with highest completed count.
     * Returns rows of [hour, count].
     */
    @Query(value = """
            SELECT HOUR(t.completed_at) as peak_hour, COUNT(t.id) as cnt
            FROM tasks t
            JOIN modules m ON t.module_id = m.id
            LEFT JOIN module_members mm ON m.id = mm.module_id AND mm.user_id = :userId
            WHERE (m.owner_id = :userId OR mm.user_id = :userId)
              AND t.status = 'COMPLETED'
              AND t.completed_at BETWEEN :from AND :to
              AND t.deleted_at IS NULL
            GROUP BY HOUR(t.completed_at)
            ORDER BY cnt DESC
            LIMIT 1
            """, nativeQuery = true)
    List<Object[]> getPeakCompletionHour(@Param("userId") Long userId,
                                         @Param("from") LocalDateTime from,
                                         @Param("to") LocalDateTime to);

    /**
     * Count of SHARED modules the user owns or is a member of.
     */
    @Query(value = """
            SELECT COUNT(DISTINCT m.id) FROM modules m
            LEFT JOIN module_members mm ON m.id = mm.module_id AND mm.user_id = :userId
            WHERE (m.owner_id = :userId OR mm.user_id = :userId)
              AND m.visibility = 'SHARED'
              AND m.deleted_at IS NULL
            """, nativeQuery = true)
    long countSharedModules(@Param("userId") Long userId);

    /**
     * Collaboration activity: tasks created or completed by members other than the module owner.
     * Returns rows of [user_id, display_name, activity_count].
     */
    @Query(value = """
            SELECT u.id as user_id, u.display_name, COUNT(t.id) as activity_count
            FROM tasks t
            JOIN modules m ON t.module_id = m.id
            JOIN users u ON t.created_by = u.id
            LEFT JOIN module_members mm ON m.id = mm.module_id AND mm.user_id = :userId
            WHERE (m.owner_id = :userId OR mm.user_id = :userId)
              AND t.created_by != m.owner_id
              AND t.created_at BETWEEN :from AND :to
              AND t.deleted_at IS NULL
            GROUP BY u.id, u.display_name
            ORDER BY activity_count DESC
            LIMIT 10
            """, nativeQuery = true)
    List<Object[]> getTopCollaborators(@Param("userId") Long userId,
                                       @Param("from") LocalDateTime from,
                                       @Param("to") LocalDateTime to);

    /**
     * Total collaboration activity count (tasks created by non-owners in accessible modules).
     */
    @Query(value = """
            SELECT COUNT(t.id) FROM tasks t
            JOIN modules m ON t.module_id = m.id
            LEFT JOIN module_members mm ON m.id = mm.module_id AND mm.user_id = :userId
            WHERE (m.owner_id = :userId OR mm.user_id = :userId)
              AND t.created_by != m.owner_id
              AND t.created_at BETWEEN :from AND :to
              AND t.deleted_at IS NULL
            """, nativeQuery = true)
    long countCollaborationActivity(@Param("userId") Long userId,
                                    @Param("from") LocalDateTime from,
                                    @Param("to") LocalDateTime to);
}
