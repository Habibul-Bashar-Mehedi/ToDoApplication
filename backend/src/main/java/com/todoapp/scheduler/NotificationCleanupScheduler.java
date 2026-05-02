package com.todoapp.scheduler;

import com.todoapp.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Scheduled job that runs daily at 02:00 to delete notifications older than 30 days.
 *
 * <p>This enforces the 30-day notification retention policy defined in Requirement 8.10.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationCleanupScheduler {

    private final NotificationRepository notificationRepository;

    /**
     * Deletes all notifications created more than 30 days ago.
     * Runs every day at 02:00 server time.
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanupOldNotifications() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
        log.info("NotificationCleanupScheduler: deleting notifications older than {}", cutoff);
        notificationRepository.deleteByCreatedAtBefore(cutoff);
        log.info("NotificationCleanupScheduler: cleanup complete");
    }
}
