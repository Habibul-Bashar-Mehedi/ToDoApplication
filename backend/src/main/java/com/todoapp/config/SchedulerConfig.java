package com.todoapp.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables Spring's scheduled task execution support.
 *
 * <p>{@code @EnableScheduling} activates the {@code @Scheduled} annotations
 * used by {@code ReminderScheduler}, {@code NotificationCleanupScheduler},
 * and {@code TokenBlacklistService}.
 */
@Configuration
@EnableScheduling
public class SchedulerConfig {
    // No additional beans required — @EnableScheduling is sufficient.
}
