package com.todoapp.scheduler;

import com.todoapp.entity.ModuleMember;
import com.todoapp.entity.NotificationType;
import com.todoapp.entity.PermissionLevel;
import com.todoapp.entity.Task;
import com.todoapp.repository.ModuleMemberRepository;
import com.todoapp.repository.NotificationPreferenceRepository;
import com.todoapp.repository.TaskRepository;
import com.todoapp.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled job that fires every minute to:
 * <ol>
 *   <li>Send reminder notifications for tasks approaching their due date.</li>
 *   <li>Send overdue notifications for tasks whose due date has passed.</li>
 * </ol>
 *
 * <p>Only PENDING tasks are considered; COMPLETED and CANCELLED tasks are
 * excluded by the repository queries.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReminderScheduler {

    private final TaskRepository taskRepository;
    private final ModuleMemberRepository moduleMemberRepository;
    private final NotificationService notificationService;
    private final NotificationPreferenceRepository notificationPreferenceRepository;

    // =========================================================================
    // Scheduled entry point
    // =========================================================================

    /**
     * Main scheduler method — runs every 60 seconds.
     */
    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void processReminders() {
        LocalDateTime now = LocalDateTime.now();
        log.debug("ReminderScheduler running at {}", now);

        process15MinReminders(now);
        process1HourReminders(now);
        process1DayReminders(now);
        process2DaysReminders(now);
        processOverdueNotifications(now);
    }

    // =========================================================================
    // Reminder processing methods
    // =========================================================================

    private void process15MinReminders(LocalDateTime now) {
        LocalDateTime windowEnd = now.plusMinutes(15);
        List<Task> tasks = taskRepository.findTasksDueInWindow(now, windowEnd);

        for (Task task : tasks) {
            if (!task.isReminder15min() || task.isReminder15minSent()) {
                continue;
            }
            sendReminderNotifications(task, NotificationType.REMINDER_15MIN,
                    "Reminder: Task \"" + task.getTitle() + "\" is due in 15 minutes.");
            task.setReminder15minSent(true);
            taskRepository.save(task);
        }
    }

    private void process1HourReminders(LocalDateTime now) {
        LocalDateTime windowEnd = now.plusHours(1);
        List<Task> tasks = taskRepository.findTasksDueInWindow(now, windowEnd);

        for (Task task : tasks) {
            if (!task.isReminder1hour() || task.isReminder1hourSent()) {
                continue;
            }
            sendReminderNotifications(task, NotificationType.REMINDER_1HOUR,
                    "Reminder: Task \"" + task.getTitle() + "\" is due in 1 hour.");
            task.setReminder1hourSent(true);
            taskRepository.save(task);
        }
    }

    private void process1DayReminders(LocalDateTime now) {
        LocalDateTime windowEnd = now.plusDays(1);
        List<Task> tasks = taskRepository.findTasksDueInWindow(now, windowEnd);

        for (Task task : tasks) {
            if (!task.isReminder1day() || task.isReminder1daySent()) {
                continue;
            }
            sendReminderNotifications(task, NotificationType.REMINDER_1DAY,
                    "Reminder: Task \"" + task.getTitle() + "\" is due in 1 day.");
            task.setReminder1daySent(true);
            taskRepository.save(task);
        }
    }

    private void process2DaysReminders(LocalDateTime now) {
        LocalDateTime windowEnd = now.plusDays(2);
        List<Task> tasks = taskRepository.findTasksDueInWindow(now, windowEnd);

        for (Task task : tasks) {
            if (!task.isReminder2days() || task.isReminder2daysSent()) {
                continue;
            }
            sendReminderNotifications(task, NotificationType.REMINDER_2DAYS,
                    "Reminder: Task \"" + task.getTitle() + "\" is due in 2 days.");
            task.setReminder2daysSent(true);
            taskRepository.save(task);
        }
    }

    private void processOverdueNotifications(LocalDateTime now) {
        List<Task> overdueTasks = taskRepository.findOverdueTasks(now);

        for (Task task : overdueTasks) {
            String message = "Task \"" + task.getTitle() + "\" is overdue.";
            sendReminderNotifications(task, NotificationType.OVERDUE, message);
            task.setOverdueNotified(true);
            taskRepository.save(task);
        }
    }

    // =========================================================================
    // Notification dispatch helper
    // =========================================================================

    /**
     * Sends a notification to the task creator and all EDIT/ADMIN module members.
     *
     * @param task    the task triggering the notification
     * @param type    the notification type
     * @param message the notification message text
     */
    private void sendReminderNotifications(Task task, NotificationType type, String message) {
        Long taskId = task.getId();

        // Notify the task creator
        Long creatorId = task.getCreatedBy().getId();
        notificationService.createNotification(creatorId, taskId, type, message);

        // Notify all EDIT and ADMIN module members (excluding the creator to avoid duplicates)
        Long moduleId = task.getModule().getId();
        List<ModuleMember> editAdminMembers = moduleMemberRepository
                .findByModuleIdAndPermissionLevelIn(
                        moduleId,
                        List.of(PermissionLevel.EDIT, PermissionLevel.ADMIN));

        for (ModuleMember member : editAdminMembers) {
            Long memberId = member.getUser().getId();
            // Skip if this member is the task creator (already notified above)
            if (memberId.equals(creatorId)) {
                continue;
            }
            notificationService.createNotification(memberId, taskId, type, message);
        }

        log.debug("Sent {} notification for task id={} to creator id={} and {} members",
                type, taskId, creatorId, editAdminMembers.size());
    }
}
