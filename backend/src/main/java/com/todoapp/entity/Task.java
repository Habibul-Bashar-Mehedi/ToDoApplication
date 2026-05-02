package com.todoapp.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "tasks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Task {

    public enum TaskStatus {
        PENDING, COMPLETED, CANCELLED
    }

    public enum Priority {
        HIGH, MEDIUM, LOW
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_id", nullable = false)
    private Module module;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_edited_by")
    private User lastEditedBy;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TaskStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false)
    private Priority priority;

    @Column(name = "due_date")
    private LocalDateTime dueDate;

    @Column(name = "reminder_15min", nullable = false, columnDefinition = "TINYINT(1)")
    private boolean reminder15min;

    @Column(name = "reminder_1hour", nullable = false, columnDefinition = "TINYINT(1)")
    private boolean reminder1hour;

    @Column(name = "reminder_1day", nullable = false, columnDefinition = "TINYINT(1)")
    private boolean reminder1day;

    @Column(name = "reminder_2days", nullable = false, columnDefinition = "TINYINT(1)")
    private boolean reminder2days;

    @Column(name = "reminder_15min_sent", nullable = false, columnDefinition = "TINYINT(1)")
    private boolean reminder15minSent;

    @Column(name = "reminder_1hour_sent", nullable = false, columnDefinition = "TINYINT(1)")
    private boolean reminder1hourSent;

    @Column(name = "reminder_1day_sent", nullable = false, columnDefinition = "TINYINT(1)")
    private boolean reminder1daySent;

    @Column(name = "reminder_2days_sent", nullable = false, columnDefinition = "TINYINT(1)")
    private boolean reminder2daysSent;

    @Column(name = "overdue_notified", nullable = false, columnDefinition = "TINYINT(1)")
    private boolean overdueNotified;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "task_tags",
        joinColumns = @JoinColumn(name = "task_id"),
        inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<Tag> tags;
}
