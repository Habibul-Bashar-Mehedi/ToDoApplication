package com.todoapp.controller;

import com.todoapp.dto.response.AnalyticsDashboardResponse;
import com.todoapp.exception.ResourceNotFoundException;
import com.todoapp.repository.UserRepository;
import com.todoapp.service.AnalyticsService;
import com.todoapp.service.ExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * REST controller for analytics dashboard and export endpoints.
 *
 * <p>Base path: {@code /analytics} (relative to the global context path {@code /api/v1}).
 * All endpoints require a valid JWT (enforced by {@code SecurityConfig}).
 */
@Tag(name = "Analytics", description = "Dashboard metrics and data export")
@RestController
@RequestMapping("/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private static final DateTimeFormatter FILENAME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final AnalyticsService analyticsService;
    private final ExportService exportService;
    private final UserRepository userRepository;

    // =========================================================================
    // GET /analytics/dashboard
    // =========================================================================

    @Operation(
        summary = "Get analytics dashboard",
        description = "Returns aggregated analytics data for the authenticated user. "
                    + "Defaults to the last 30 days if no date range is provided."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Dashboard data retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/dashboard")
    public ResponseEntity<AnalyticsDashboardResponse> getDashboard(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @AuthenticationPrincipal UserDetails userDetails) {

        LocalDateTime effectiveTo = to != null ? to : LocalDateTime.now();
        LocalDateTime effectiveFrom = from != null ? from : effectiveTo.minusDays(30);

        Long userId = getCurrentUserId(userDetails);
        AnalyticsDashboardResponse response = analyticsService.getDashboard(userId, effectiveFrom, effectiveTo);
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // GET /analytics/export
    // =========================================================================

    @Operation(
        summary = "Export analytics data",
        description = "Exports analytics dashboard data as PDF or CSV. "
                    + "Use ?format=pdf or ?format=csv. Date range defaults to last 30 days."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Export generated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid or missing format parameter"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportDashboard(
            @RequestParam String format,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @AuthenticationPrincipal UserDetails userDetails) {

        LocalDateTime effectiveTo = to != null ? to : LocalDateTime.now();
        LocalDateTime effectiveFrom = from != null ? from : effectiveTo.minusDays(30);

        Long userId = getCurrentUserId(userDetails);
        String userDisplayName = getUserDisplayName(userDetails);

        AnalyticsDashboardResponse dashboard = analyticsService.getDashboard(userId, effectiveFrom, effectiveTo);

        String fromStr = effectiveFrom.format(FILENAME_FMT);
        String toStr = effectiveTo.format(FILENAME_FMT);

        return switch (format.toLowerCase()) {
            case "pdf" -> {
                byte[] pdfBytes = exportService.exportPdf(dashboard, userDisplayName);
                yield ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_PDF)
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                "attachment; filename=analytics-" + fromStr + "-" + toStr + ".pdf")
                        .body(pdfBytes);
            }
            case "csv" -> {
                String csvContent = exportService.exportCsv(dashboard);
                byte[] csvBytes = csvContent.getBytes(StandardCharsets.UTF_8);
                yield ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType("text/csv"))
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                "attachment; filename=analytics-" + fromStr + "-" + toStr + ".csv")
                        .body(csvBytes);
            }
            default -> ResponseEntity.badRequest()
                    .<byte[]>build();
        };
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    /**
     * Resolves the current user's database ID from their {@link UserDetails} (email as username).
     */
    private Long getCurrentUserId(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userDetails.getUsername()))
                .getId();
    }

    /**
     * Resolves the current user's display name from their {@link UserDetails}.
     */
    private String getUserDisplayName(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userDetails.getUsername()))
                .getDisplayName();
    }
}
