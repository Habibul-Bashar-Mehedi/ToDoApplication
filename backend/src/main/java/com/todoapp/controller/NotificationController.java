package com.todoapp.controller;

import com.todoapp.dto.request.NotificationPreferenceRequest;
import com.todoapp.dto.response.NotificationPreferenceResponse;
import com.todoapp.dto.response.NotificationResponse;
import com.todoapp.exception.ResourceNotFoundException;
import com.todoapp.repository.UserRepository;
import com.todoapp.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for notification management endpoints.
 *
 * <p>Base path: {@code /notifications} (relative to the global context path {@code /api/v1}).
 * All endpoints require a valid JWT (enforced by {@code SecurityConfig}).
 */
@Tag(name = "Notifications", description = "Manage in-app notifications and notification preferences")
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    // =========================================================================
    // GET /notifications
    // =========================================================================

    @Operation(
        summary = "List notifications",
        description = "Returns a paginated list of notifications for the authenticated user, "
                    + "ordered by creation date descending."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Notifications retrieved successfully")
    })
    @GetMapping
    public ResponseEntity<Page<NotificationResponse>> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = getCurrentUserId(userDetails);
        Pageable pageable = PageRequest.of(page, size);
        Page<NotificationResponse> notifications =
                notificationService.getNotifications(userId, pageable);
        return ResponseEntity.ok(notifications);
    }

    // =========================================================================
    // PATCH /notifications/{id}/read
    // =========================================================================

    @Operation(
        summary = "Mark notification as read",
        description = "Sets the is_read flag to true for the specified notification. "
                    + "Returns 403 if the notification belongs to a different user."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Notification marked as read"),
        @ApiResponse(responseCode = "403", description = "Notification belongs to a different user"),
        @ApiResponse(responseCode = "404", description = "Notification not found")
    })
    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markRead(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = getCurrentUserId(userDetails);
        notificationService.markRead(id, userId);
        return ResponseEntity.noContent().build();
    }

    // =========================================================================
    // POST /notifications/read-all
    // =========================================================================

    @Operation(
        summary = "Mark all notifications as read",
        description = "Sets is_read=true on all notifications belonging to the authenticated user."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "All notifications marked as read")
    })
    @PostMapping("/read-all")
    public ResponseEntity<Void> markAllRead(
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = getCurrentUserId(userDetails);
        notificationService.markAllRead(userId);
        return ResponseEntity.noContent().build();
    }

    // =========================================================================
    // GET /notifications/preferences
    // =========================================================================

    @Operation(
        summary = "Get notification preferences",
        description = "Returns the notification preferences for the authenticated user. "
                    + "Creates default preferences (all enabled) if none exist."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Preferences retrieved successfully")
    })
    @GetMapping("/preferences")
    public ResponseEntity<NotificationPreferenceResponse> getPreferences(
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = getCurrentUserId(userDetails);
        NotificationPreferenceResponse preferences = notificationService.getPreferences(userId);
        return ResponseEntity.ok(preferences);
    }

    // =========================================================================
    // PUT /notifications/preferences
    // =========================================================================

    @Operation(
        summary = "Update notification preferences",
        description = "Updates the notification preferences for the authenticated user."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Preferences updated successfully"),
        @ApiResponse(responseCode = "400", description = "Validation failure")
    })
    @PutMapping("/preferences")
    public ResponseEntity<NotificationPreferenceResponse> updatePreferences(
            @Valid @RequestBody NotificationPreferenceRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = getCurrentUserId(userDetails);
        NotificationPreferenceResponse updated =
                notificationService.updatePreferences(userId, request);
        return ResponseEntity.ok(updated);
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    /**
     * Resolves the current user's database ID from their {@link UserDetails} (email as username).
     */
    private Long getCurrentUserId(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User", "email", userDetails.getUsername()))
                .getId();
    }
}
