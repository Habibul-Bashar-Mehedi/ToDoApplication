package com.todoapp.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsDashboardResponse {

    private DateRange dateRange;
    private Summary summary;
    private List<StatusDistributionEntry> statusDistribution;
    private List<CompletionTrendEntry> completionTrend;
    private List<ModulePerformanceEntry> modulePerformance;
    private List<PriorityAnalysisEntry> priorityAnalysis;
    private double avgCompletionDays;
    private List<OverdueTrendEntry> overdueTrend;
    private ProductivityInsights productivityInsights;
    private SharingMetrics sharingMetrics;

    // -------------------------------------------------------------------------
    // Nested types
    // -------------------------------------------------------------------------

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DateRange {
        private LocalDateTime from;
        private LocalDateTime to;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Summary {
        private int totalTasks;
        /** Completion rate as a value between 0.0 and 1.0. */
        private double completionRate;
        private int overdueCount;
        private int activeModuleCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusDistributionEntry {
        private String status;
        private int count;
        private double percentage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CompletionTrendEntry {
        /** Date label (day, week, or month depending on range length). */
        private String date;
        private int created;
        private int completed;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ModulePerformanceEntry {
        private Long moduleId;
        private String moduleName;
        /** Completion rate as a value between 0.0 and 1.0. */
        private double completionRate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PriorityAnalysisEntry {
        private String priority;
        private int count;
        /** Completion rate as a value between 0.0 and 1.0. */
        private double completionRate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OverdueTrendEntry {
        private String date;
        private int overdueCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductivityInsights {
        private String mostProductiveDayOfWeek;
        /** Hour of day with highest completion count (0–23). */
        private int peakCompletionHour;
        /** Tasks completed per week over the selected date range. */
        private double completionVelocityPerWeek;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SharingMetrics {
        private int sharedModuleCount;
        private int collaborationActivityCount;
        private List<CollaboratorEntry> topCollaborators;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CollaboratorEntry {
        private Long userId;
        private String displayName;
        private int activityCount;
    }
}
