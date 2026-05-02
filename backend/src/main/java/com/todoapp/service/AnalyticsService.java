package com.todoapp.service;

import com.todoapp.dto.response.AnalyticsDashboardResponse;
import com.todoapp.dto.response.AnalyticsDashboardResponse.*;
import com.todoapp.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Service that computes analytics dashboard data for a given user and date range.
 *
 * <p>All queries are scoped to modules the user owns or is a member of.
 * Results are cached for 5 minutes using Caffeine (cache name: "analytics").
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final TaskRepository taskRepository;

    /**
     * Builds the full analytics dashboard response for the given user and date range.
     *
     * @param userId the authenticated user's ID
     * @param from   start of the date range (inclusive)
     * @param to     end of the date range (inclusive)
     * @return the populated {@link AnalyticsDashboardResponse}
     */
    @Cacheable(value = "analytics", key = "#userId + '_' + #from + '_' + #to")
    @Transactional(readOnly = true)
    public AnalyticsDashboardResponse getDashboard(Long userId, LocalDateTime from, LocalDateTime to) {
        log.debug("Computing analytics dashboard for userId={}, from={}, to={}", userId, from, to);

        return AnalyticsDashboardResponse.builder()
                .dateRange(buildDateRange(from, to))
                .summary(buildSummary(userId, from, to))
                .statusDistribution(buildStatusDistribution(userId, from, to))
                .completionTrend(buildCompletionTrend(userId, from, to))
                .modulePerformance(buildModulePerformance(userId))
                .priorityAnalysis(buildPriorityAnalysis(userId, from, to))
                .avgCompletionDays(buildAvgCompletionDays(userId, from, to))
                .overdueTrend(buildOverdueTrend(userId, from, to))
                .productivityInsights(buildProductivityInsights(userId, from, to))
                .sharingMetrics(buildSharingMetrics(userId, from, to))
                .build();
    }

    // =========================================================================
    // 1. Date range
    // =========================================================================

    private DateRange buildDateRange(LocalDateTime from, LocalDateTime to) {
        return DateRange.builder().from(from).to(to).build();
    }

    // =========================================================================
    // 2. Summary metrics
    // =========================================================================

    private Summary buildSummary(Long userId, LocalDateTime from, LocalDateTime to) {
        long totalTasks = taskRepository.countTasksInRange(userId, from, to);
        long completedCount = taskRepository.countCompletedTasksInRange(userId, from, to);
        long cancelledCount = taskRepository.countCancelledTasksInRange(userId, from, to);
        long overdueCount = taskRepository.countOverdueTasks(userId, LocalDateTime.now());
        long activeModuleCount = taskRepository.countActiveModules(userId);

        // Completion rate: completed / (total - cancelled); avoid division by zero
        long denominator = totalTasks - cancelledCount;
        double completionRate = denominator > 0 ? (double) completedCount / denominator : 0.0;

        return Summary.builder()
                .totalTasks((int) totalTasks)
                .completionRate(completionRate)
                .overdueCount((int) overdueCount)
                .activeModuleCount((int) activeModuleCount)
                .build();
    }

    // =========================================================================
    // 3. Status distribution
    // =========================================================================

    private List<StatusDistributionEntry> buildStatusDistribution(Long userId,
                                                                    LocalDateTime from,
                                                                    LocalDateTime to) {
        List<Object[]> rows = taskRepository.countTasksByStatus(userId, from, to);
        long total = rows.stream()
                .mapToLong(r -> ((Number) r[1]).longValue())
                .sum();

        List<StatusDistributionEntry> result = new ArrayList<>();
        for (Object[] row : rows) {
            String status = (String) row[0];
            int count = ((Number) row[1]).intValue();
            double percentage = total > 0 ? (double) count / total * 100.0 : 0.0;
            result.add(StatusDistributionEntry.builder()
                    .status(status)
                    .count(count)
                    .percentage(percentage)
                    .build());
        }
        return result;
    }

    // =========================================================================
    // 4. Completion trend (adaptive granularity)
    // =========================================================================

    private List<CompletionTrendEntry> buildCompletionTrend(Long userId,
                                                             LocalDateTime from,
                                                             LocalDateTime to) {
        long days = ChronoUnit.DAYS.between(from, to);
        List<Object[]> rows;

        if (days <= 14) {
            rows = taskRepository.getDailyTrend(userId, from, to);
        } else if (days <= 90) {
            rows = taskRepository.getWeeklyTrend(userId, from, to);
        } else {
            rows = taskRepository.getMonthlyTrend(userId, from, to);
        }

        return mapTrendRows(rows);
    }

    private List<CompletionTrendEntry> mapTrendRows(List<Object[]> rows) {
        List<CompletionTrendEntry> result = new ArrayList<>();
        for (Object[] row : rows) {
            String dateLabel = row[0] != null ? row[0].toString() : "";
            int created = ((Number) row[1]).intValue();
            int completed = row[2] != null ? ((Number) row[2]).intValue() : 0;
            result.add(CompletionTrendEntry.builder()
                    .date(dateLabel)
                    .created(created)
                    .completed(completed)
                    .build());
        }
        return result;
    }

    // =========================================================================
    // 5. Module performance
    // =========================================================================

    private List<ModulePerformanceEntry> buildModulePerformance(Long userId) {
        List<Object[]> rows = taskRepository.getModulePerformance(userId);
        List<ModulePerformanceEntry> result = new ArrayList<>();
        for (Object[] row : rows) {
            Long moduleId = ((Number) row[0]).longValue();
            String moduleName = (String) row[1];
            long totalCount = ((Number) row[2]).longValue();
            long completedCount = row[3] != null ? ((Number) row[3]).longValue() : 0L;
            long cancelledCount = row[4] != null ? ((Number) row[4]).longValue() : 0L;

            long denominator = Math.max(1, totalCount - cancelledCount);
            double completionRate = (double) completedCount / denominator;

            result.add(ModulePerformanceEntry.builder()
                    .moduleId(moduleId)
                    .moduleName(moduleName)
                    .completionRate(completionRate)
                    .build());
        }
        return result;
    }

    // =========================================================================
    // 6. Priority analysis
    // =========================================================================

    private List<PriorityAnalysisEntry> buildPriorityAnalysis(Long userId,
                                                               LocalDateTime from,
                                                               LocalDateTime to) {
        List<Object[]> rows = taskRepository.getPriorityAnalysis(userId, from, to);
        List<PriorityAnalysisEntry> result = new ArrayList<>();
        for (Object[] row : rows) {
            String priority = (String) row[0];
            int totalCount = ((Number) row[1]).intValue();
            long completedCount = row[2] != null ? ((Number) row[2]).longValue() : 0L;
            long cancelledCount = row[3] != null ? ((Number) row[3]).longValue() : 0L;

            long denominator = Math.max(1, totalCount - cancelledCount);
            double completionRate = (double) completedCount / denominator;

            result.add(PriorityAnalysisEntry.builder()
                    .priority(priority)
                    .count(totalCount)
                    .completionRate(completionRate)
                    .build());
        }
        return result;
    }

    // =========================================================================
    // 7. Average completion time
    // =========================================================================

    private double buildAvgCompletionDays(Long userId, LocalDateTime from, LocalDateTime to) {
        Double avg = taskRepository.getAvgCompletionDays(userId, from, to);
        return avg != null ? avg : 0.0;
    }

    // =========================================================================
    // 8. Overdue trend (adaptive granularity)
    // =========================================================================

    private List<OverdueTrendEntry> buildOverdueTrend(Long userId,
                                                       LocalDateTime from,
                                                       LocalDateTime to) {
        long days = ChronoUnit.DAYS.between(from, to);
        LocalDateTime now = LocalDateTime.now();
        List<Object[]> rows;

        if (days <= 14) {
            rows = taskRepository.getDailyOverdueTrend(userId, from, to, now);
        } else if (days <= 90) {
            rows = taskRepository.getWeeklyOverdueTrend(userId, from, to, now);
        } else {
            rows = taskRepository.getMonthlyOverdueTrend(userId, from, to, now);
        }

        List<OverdueTrendEntry> result = new ArrayList<>();
        for (Object[] row : rows) {
            String dateLabel = row[0] != null ? row[0].toString() : "";
            int overdueCount = ((Number) row[1]).intValue();
            result.add(OverdueTrendEntry.builder()
                    .date(dateLabel)
                    .overdueCount(overdueCount)
                    .build());
        }
        return result;
    }

    // =========================================================================
    // 9. Productivity insights
    // =========================================================================

    private ProductivityInsights buildProductivityInsights(Long userId,
                                                            LocalDateTime from,
                                                            LocalDateTime to) {
        // Most productive day
        List<Object[]> dayRows = taskRepository.getMostProductiveDay(userId, from, to);
        String mostProductiveDay = dayRows.isEmpty() ? null : (String) dayRows.get(0)[0];

        // Peak completion hour
        List<Object[]> hourRows = taskRepository.getPeakCompletionHour(userId, from, to);
        int peakHour = hourRows.isEmpty() ? 0 : ((Number) hourRows.get(0)[0]).intValue();

        // Velocity: completed tasks / (range days / 7.0)
        long completedCount = taskRepository.countCompletedTasksInRange(userId, from, to);
        long rangeDays = Math.max(1, ChronoUnit.DAYS.between(from, to));
        double velocity = completedCount / (rangeDays / 7.0);

        return ProductivityInsights.builder()
                .mostProductiveDayOfWeek(mostProductiveDay)
                .peakCompletionHour(peakHour)
                .completionVelocityPerWeek(velocity)
                .build();
    }

    // =========================================================================
    // 10. Sharing metrics
    // =========================================================================

    private SharingMetrics buildSharingMetrics(Long userId, LocalDateTime from, LocalDateTime to) {
        long sharedModuleCount = taskRepository.countSharedModules(userId);
        long collaborationActivity = taskRepository.countCollaborationActivity(userId, from, to);

        List<Object[]> collaboratorRows = taskRepository.getTopCollaborators(userId, from, to);
        List<CollaboratorEntry> topCollaborators = new ArrayList<>();
        for (Object[] row : collaboratorRows) {
            Long collabUserId = ((Number) row[0]).longValue();
            String displayName = (String) row[1];
            int activityCount = ((Number) row[2]).intValue();
            topCollaborators.add(CollaboratorEntry.builder()
                    .userId(collabUserId)
                    .displayName(displayName)
                    .activityCount(activityCount)
                    .build());
        }

        return SharingMetrics.builder()
                .sharedModuleCount((int) sharedModuleCount)
                .collaborationActivityCount((int) collaborationActivity)
                .topCollaborators(topCollaborators)
                .build();
    }
}
