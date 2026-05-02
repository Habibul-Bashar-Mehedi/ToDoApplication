package com.todoapp.service;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.todoapp.dto.response.AnalyticsDashboardResponse;
import com.todoapp.dto.response.AnalyticsDashboardResponse.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Service for exporting analytics dashboard data as PDF or CSV.
 */
@Slf4j
@Service
public class ExportService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // =========================================================================
    // PDF Export
    // =========================================================================

    /**
     * Generates a PDF document from the analytics dashboard data.
     *
     * @param dashboard       the analytics data
     * @param userDisplayName the authenticated user's display name
     * @return the PDF as a byte array
     */
    public byte[] exportPdf(AnalyticsDashboardResponse dashboard, String userDisplayName) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (PdfWriter writer = new PdfWriter(baos);
             PdfDocument pdfDoc = new PdfDocument(writer);
             Document document = new Document(pdfDoc)) {

            // ---- Cover page ----
            document.add(new Paragraph("Analytics Dashboard")
                    .setFontSize(24)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph("User: " + userDisplayName)
                    .setFontSize(14)
                    .setTextAlignment(TextAlignment.CENTER));

            if (dashboard.getDateRange() != null) {
                String from = dashboard.getDateRange().getFrom() != null
                        ? dashboard.getDateRange().getFrom().format(DATE_FMT) : "N/A";
                String to = dashboard.getDateRange().getTo() != null
                        ? dashboard.getDateRange().getTo().format(DATE_FMT) : "N/A";
                document.add(new Paragraph("Date Range: " + from + " to " + to)
                        .setFontSize(12)
                        .setTextAlignment(TextAlignment.CENTER));
            }

            document.add(new Paragraph("Generated: " + java.time.LocalDateTime.now().format(DATETIME_FMT))
                    .setFontSize(10)
                    .setTextAlignment(TextAlignment.CENTER));

            document.add(new Paragraph("\n"));

            // ---- Summary metrics ----
            document.add(sectionHeader("Summary Metrics"));
            if (dashboard.getSummary() != null) {
                Summary s = dashboard.getSummary();
                Table summaryTable = createTable(new String[]{"Metric", "Value"});
                addRow(summaryTable, "Total Tasks", String.valueOf(s.getTotalTasks()));
                addRow(summaryTable, "Completion Rate (%)",
                        String.format("%.1f", s.getCompletionRate() * 100));
                addRow(summaryTable, "Overdue Count", String.valueOf(s.getOverdueCount()));
                addRow(summaryTable, "Active Module Count", String.valueOf(s.getActiveModuleCount()));
                document.add(summaryTable);
            }

            document.add(new Paragraph("\n"));

            // ---- Status distribution ----
            document.add(sectionHeader("Status Distribution"));
            if (dashboard.getStatusDistribution() != null && !dashboard.getStatusDistribution().isEmpty()) {
                Table statusTable = createTable(new String[]{"Status", "Count", "Percentage (%)"});
                for (StatusDistributionEntry entry : dashboard.getStatusDistribution()) {
                    addRow(statusTable,
                            entry.getStatus(),
                            String.valueOf(entry.getCount()),
                            String.format("%.1f", entry.getPercentage()));
                }
                document.add(statusTable);
            }

            document.add(new Paragraph("\n"));

            // ---- Module performance ----
            document.add(sectionHeader("Module Performance"));
            if (dashboard.getModulePerformance() != null && !dashboard.getModulePerformance().isEmpty()) {
                Table moduleTable = createTable(new String[]{"Module Name", "Completion Rate (%)"});
                for (ModulePerformanceEntry entry : dashboard.getModulePerformance()) {
                    addRow(moduleTable,
                            entry.getModuleName(),
                            String.format("%.1f", entry.getCompletionRate() * 100));
                }
                document.add(moduleTable);
            }

            document.add(new Paragraph("\n"));

            // ---- Priority analysis ----
            document.add(sectionHeader("Priority Analysis"));
            if (dashboard.getPriorityAnalysis() != null && !dashboard.getPriorityAnalysis().isEmpty()) {
                Table priorityTable = createTable(new String[]{"Priority", "Count", "Completion Rate (%)"});
                for (PriorityAnalysisEntry entry : dashboard.getPriorityAnalysis()) {
                    addRow(priorityTable,
                            entry.getPriority(),
                            String.valueOf(entry.getCount()),
                            String.format("%.1f", entry.getCompletionRate() * 100));
                }
                document.add(priorityTable);
            }

            document.add(new Paragraph("\n"));

            // ---- Productivity insights ----
            document.add(sectionHeader("Productivity Insights"));
            if (dashboard.getProductivityInsights() != null) {
                ProductivityInsights pi = dashboard.getProductivityInsights();
                Table insightsTable = createTable(new String[]{"Metric", "Value"});
                addRow(insightsTable, "Most Productive Day",
                        pi.getMostProductiveDayOfWeek() != null ? pi.getMostProductiveDayOfWeek() : "N/A");
                addRow(insightsTable, "Peak Hour", String.valueOf(pi.getPeakCompletionHour()));
                addRow(insightsTable, "Velocity (tasks/week)",
                        String.format("%.2f", pi.getCompletionVelocityPerWeek()));
                document.add(insightsTable);
            }

            document.add(new Paragraph("\n"));

            // ---- Sharing metrics ----
            document.add(sectionHeader("Sharing Metrics"));
            if (dashboard.getSharingMetrics() != null) {
                SharingMetrics sm = dashboard.getSharingMetrics();
                Table sharingTable = createTable(new String[]{"Metric", "Value"});
                addRow(sharingTable, "Shared Modules", String.valueOf(sm.getSharedModuleCount()));
                addRow(sharingTable, "Collaboration Activity", String.valueOf(sm.getCollaborationActivityCount()));
                document.add(sharingTable);

                if (sm.getTopCollaborators() != null && !sm.getTopCollaborators().isEmpty()) {
                    document.add(new Paragraph("Top Collaborators").setBold().setFontSize(11));
                    Table collabTable = createTable(new String[]{"User", "Activity Count"});
                    for (CollaboratorEntry entry : sm.getTopCollaborators()) {
                        addRow(collabTable,
                                entry.getDisplayName(),
                                String.valueOf(entry.getActivityCount()));
                    }
                    document.add(collabTable);
                }
            }

        } catch (Exception e) {
            log.error("Failed to generate PDF export", e);
            throw new RuntimeException("PDF generation failed: " + e.getMessage(), e);
        }

        return baos.toByteArray();
    }

    // =========================================================================
    // CSV Export
    // =========================================================================

    /**
     * Generates a CSV string from the analytics dashboard data.
     *
     * @param dashboard the analytics data
     * @return the CSV content as a String
     */
    public String exportCsv(AnalyticsDashboardResponse dashboard) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        // ---- SUMMARY ----
        pw.println("SUMMARY");
        pw.println("Metric,Value");
        if (dashboard.getSummary() != null) {
            Summary s = dashboard.getSummary();
            pw.println("Total Tasks," + s.getTotalTasks());
            pw.println("Completion Rate (%)," + String.format("%.1f", s.getCompletionRate() * 100));
            pw.println("Overdue Count," + s.getOverdueCount());
            pw.println("Active Module Count," + s.getActiveModuleCount());
        }
        pw.println();

        // ---- STATUS DISTRIBUTION ----
        pw.println("STATUS DISTRIBUTION");
        pw.println("Status,Count,Percentage (%)");
        if (dashboard.getStatusDistribution() != null) {
            for (StatusDistributionEntry entry : dashboard.getStatusDistribution()) {
                pw.println(csvEscape(entry.getStatus()) + ","
                        + entry.getCount() + ","
                        + String.format("%.1f", entry.getPercentage()));
            }
        }
        pw.println();

        // ---- COMPLETION TREND ----
        pw.println("COMPLETION TREND");
        pw.println("Date,Created,Completed");
        if (dashboard.getCompletionTrend() != null) {
            for (CompletionTrendEntry entry : dashboard.getCompletionTrend()) {
                pw.println(csvEscape(entry.getDate()) + ","
                        + entry.getCreated() + ","
                        + entry.getCompleted());
            }
        }
        pw.println();

        // ---- MODULE PERFORMANCE ----
        pw.println("MODULE PERFORMANCE");
        pw.println("Module Name,Completion Rate (%)");
        if (dashboard.getModulePerformance() != null) {
            for (ModulePerformanceEntry entry : dashboard.getModulePerformance()) {
                pw.println(csvEscape(entry.getModuleName()) + ","
                        + String.format("%.1f", entry.getCompletionRate() * 100));
            }
        }
        pw.println();

        // ---- PRIORITY ANALYSIS ----
        pw.println("PRIORITY ANALYSIS");
        pw.println("Priority,Count,Completion Rate (%)");
        if (dashboard.getPriorityAnalysis() != null) {
            for (PriorityAnalysisEntry entry : dashboard.getPriorityAnalysis()) {
                pw.println(csvEscape(entry.getPriority()) + ","
                        + entry.getCount() + ","
                        + String.format("%.1f", entry.getCompletionRate() * 100));
            }
        }
        pw.println();

        // ---- PRODUCTIVITY INSIGHTS ----
        pw.println("PRODUCTIVITY INSIGHTS");
        pw.println("Metric,Value");
        if (dashboard.getProductivityInsights() != null) {
            ProductivityInsights pi = dashboard.getProductivityInsights();
            pw.println("Most Productive Day," + csvEscape(
                    pi.getMostProductiveDayOfWeek() != null ? pi.getMostProductiveDayOfWeek() : "N/A"));
            pw.println("Peak Hour," + pi.getPeakCompletionHour());
            pw.println("Velocity (tasks/week)," + String.format("%.2f", pi.getCompletionVelocityPerWeek()));
        }
        pw.println();

        // ---- SHARING METRICS ----
        pw.println("SHARING METRICS");
        pw.println("Metric,Value");
        if (dashboard.getSharingMetrics() != null) {
            SharingMetrics sm = dashboard.getSharingMetrics();
            pw.println("Shared Modules," + sm.getSharedModuleCount());
            pw.println("Collaboration Activity," + sm.getCollaborationActivityCount());
            if (sm.getTopCollaborators() != null && !sm.getTopCollaborators().isEmpty()) {
                pw.println();
                pw.println("Top Collaborators");
                pw.println("User,Activity Count");
                for (CollaboratorEntry entry : sm.getTopCollaborators()) {
                    pw.println(csvEscape(entry.getDisplayName()) + "," + entry.getActivityCount());
                }
            }
        }

        pw.flush();
        return sw.toString();
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private Paragraph sectionHeader(String title) {
        return new Paragraph(title)
                .setBold()
                .setFontSize(14)
                .setFontColor(ColorConstants.DARK_GRAY);
    }

    private Table createTable(String[] headers) {
        Table table = new Table(UnitValue.createPercentArray(headers.length))
                .useAllAvailableWidth();
        for (String header : headers) {
            table.addHeaderCell(new Cell()
                    .add(new Paragraph(header).setBold())
                    .setBackgroundColor(ColorConstants.LIGHT_GRAY));
        }
        return table;
    }

    private void addRow(Table table, String... values) {
        for (String value : values) {
            table.addCell(new Cell().add(new Paragraph(value != null ? value : "")));
        }
    }

    /**
     * Escapes a CSV field value by wrapping in quotes if it contains commas, quotes, or newlines.
     */
    private String csvEscape(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
