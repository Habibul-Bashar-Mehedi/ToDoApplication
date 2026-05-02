package com.todoapp.service;

import com.todoapp.dto.request.NotificationPreferenceRequest;
import com.todoapp.dto.response.NotificationPreferenceResponse;
import com.todoapp.dto.response.NotificationResponse;
import com.todoapp.entity.Notification;
import com.todoapp.entity.NotificationPreference;
import com.todoapp.entity.NotificationType;
import com.todoapp.entity.Task;
import com.todoapp.entity.User;
import com.todoapp.exception.ForbiddenException;
import com.todoapp.exception.ResourceNotFoundException;
import com.todoapp.mapper.NotificationMapper;
import com.todoapp.repository.NotificationPreferenceRepository;
import com.todoapp.repository.NotificationRepository;
import com.todoapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for creating and managing in-app notifications and notification preferences.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationPreferenceRepository notificationPreferenceRepository;
    private final UserRepository userRepository;
    private final NotificationMapper notificationMapper;

    // =========================================================================
    // Notification retrieval
    // =========================================================================

    /**
     * Returns a paginated list of notifications for the given user, ordered by
     * {@code created_at} DESC.
     *
     * @param userId   the authenticated user's ID
     * @param pageable pagination parameters
     * @return a page of {@link NotificationResponse} DTOs
     */
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getNotifications(Long userId, Pageable pageable) {
        return notificationRepository
                .findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(notificationMapper::toResponse);
    }

    /**
     * Returns the count of unread notifications for the given user.
     *
     * @param userId the authenticated user's ID
     * @return unread notification count
     */
    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    // =========================================================================
    // Mark read
    // =========================================================================

    /**
     * Marks a single notification as read.
     *
     * @param notificationId the notification to mark
     * @param userId         the authenticated user's ID (ownership check)
     * @throws ResourceNotFoundException if the notification does not exist
     * @throws ForbiddenException        if the notification belongs to a different user
     */
    @Transactional
    public void markRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Notification", "id", notificationId));

        if (!notification.getUser().getId().equals(userId)) {
            throw new ForbiddenException("You do not have permission to modify this notification");
        }

        notification.setRead(true);
        notificationRepository.save(notification);
        log.debug("Marked notification id={} as read for user id={}", notificationId, userId);
    }

    /**
     * Marks all notifications for the given user as read.
     *
     * @param userId the authenticated user's ID
     */
    @Transactional
    public void markAllRead(Long userId) {
        notificationRepository.markAllReadByUserId(userId);
        log.debug("Marked all notifications as read for user id={}", userId);
    }

    // =========================================================================
    // Preferences
    // =========================================================================

    /**
     * Returns the notification preferences for the given user.
     * Creates a default preference record (all enabled) if one does not yet exist.
     *
     * @param userId the authenticated user's ID
     * @return the user's {@link NotificationPreferenceResponse}
     */
    @Transactional
    public NotificationPreferenceResponse getPreferences(Long userId) {
        NotificationPreference preference = notificationPreferenceRepository
                .findByUserId(userId)
                .orElseGet(() -> createDefaultPreferences(userId));
        return notificationMapper.toPreferenceResponse(preference);
    }

    /**
     * Updates the notification preferences for the given user.
     *
     * @param userId  the authenticated user's ID
     * @param request the new preference values
     * @return the updated {@link NotificationPreferenceResponse}
     */
    @Transactional
    public NotificationPreferenceResponse updatePreferences(Long userId,
                                                             NotificationPreferenceRequest request) {
        NotificationPreference preference = notificationPreferenceRepository
                .findByUserId(userId)
                .orElseGet(() -> createDefaultPreferences(userId));

        preference.setReminder15minEnabled(request.getReminder15minEnabled());
        preference.setReminder1hourEnabled(request.getReminder1hourEnabled());
        preference.setReminder1dayEnabled(request.getReminder1dayEnabled());
        preference.setReminder2daysEnabled(request.getReminder2daysEnabled());
        preference.setOverdueEnabled(request.getOverdueEnabled());

        NotificationPreference saved = notificationPreferenceRepository.save(preference);
        log.debug("Updated notification preferences for user id={}", userId);
        return notificationMapper.toPreferenceResponse(saved);
    }

    // =========================================================================
    // Internal notification creation
    // =========================================================================

    /**
     * Creates and persists an in-app notification for the given user, after
     * checking the user's notification preferences.
     *
     * <p>Preference check rules:
     * <ul>
     *   <li>{@code REMINDER_15MIN} → {@code reminder15minEnabled}</li>
     *   <li>{@code REMINDER_1HOUR} → {@code reminder1hourEnabled}</li>
     *   <li>{@code REMINDER_1DAY}  → {@code reminder1dayEnabled}</li>
     *   <li>{@code REMINDER_2DAYS} → {@code reminder2daysEnabled}</li>
     *   <li>{@code OVERDUE}        → {@code overdueEnabled}</li>
     *   <li>{@code INVITATION} and {@code SYSTEM} → always created</li>
     * </ul>
     *
     * @param userId  the recipient user's ID
     * @param taskId  the related task's ID (may be {@code null})
     * @param type    the notification type
     * @param message the notification message
     * @return the saved {@link Notification}, or {@code null} if suppressed by preferences
     */
    @Transactional
    public Notification createNotification(Long userId, Long taskId,
                                            NotificationType type, String message) {
        // Check preferences for reminder/overdue types
        if (isPreferenceControlled(type)) {
            NotificationPreference pref = notificationPreferenceRepository
                    .findByUserId(userId)
                    .orElseGet(() -> createDefaultPreferences(userId));

            if (!isPreferenceEnabled(pref, type)) {
                log.debug("Suppressed {} notification for user id={} (preference disabled)", type, userId);
                return null;
            }
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Task task = null;
        if (taskId != null) {
            // We use a lazy reference — the task entity is already managed in the same session
            // when called from the scheduler, so we just build a proxy reference.
            task = new Task();
            task.setId(taskId);
        }

        Notification notification = Notification.builder()
                .user(user)
                .task(task)
                .type(type)
                .message(message)
                .isRead(false)
                .build();

        Notification saved = notificationRepository.save(notification);
        log.debug("Created notification id={} type={} for user id={}", saved.getId(), type, userId);
        return saved;
    }

    /**
     * Legacy overload that accepts entity references directly (used by existing callers
     * such as {@code InvitationService}).
     *
     * @param user    the recipient user entity
     * @param task    the related task entity (may be {@code null})
     * @param type    the notification type
     * @param message the notification message
     * @return the saved {@link Notification}, or {@code null} if suppressed by preferences
     */
    @Transactional
    public Notification createNotification(User user, Task task,
                                            NotificationType type, String message) {
        Long taskId = (task != null) ? task.getId() : null;
        return createNotification(user.getId(), taskId, type, message);
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    /**
     * Returns {@code true} for notification types that are controlled by user preferences.
     */
    private boolean isPreferenceControlled(NotificationType type) {
        return switch (type) {
            case REMINDER_15MIN, REMINDER_1HOUR, REMINDER_1DAY, REMINDER_2DAYS, OVERDUE -> true;
            case INVITATION, SYSTEM -> false;
        };
    }

    /**
     * Returns {@code true} if the user's preference for the given type is enabled.
     */
    private boolean isPreferenceEnabled(NotificationPreference pref, NotificationType type) {
        return switch (type) {
            case REMINDER_15MIN -> pref.isReminder15minEnabled();
            case REMINDER_1HOUR -> pref.isReminder1hourEnabled();
            case REMINDER_1DAY  -> pref.isReminder1dayEnabled();
            case REMINDER_2DAYS -> pref.isReminder2daysEnabled();
            case OVERDUE        -> pref.isOverdueEnabled();
            default             -> true;
        };
    }

    /**
     * Creates and persists a default {@link NotificationPreference} (all enabled) for the user.
     */
    private NotificationPreference createDefaultPreferences(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        NotificationPreference pref = NotificationPreference.builder()
                .user(user)
                .reminder15minEnabled(true)
                .reminder1hourEnabled(true)
                .reminder1dayEnabled(true)
                .reminder2daysEnabled(true)
                .overdueEnabled(true)
                .build();

        return notificationPreferenceRepository.save(pref);
    }
}
