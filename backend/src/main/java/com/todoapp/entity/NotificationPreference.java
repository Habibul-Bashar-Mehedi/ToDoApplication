package com.todoapp.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "notification_preferences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "reminder_15min_enabled", nullable = false, columnDefinition = "TINYINT(1)")
    private boolean reminder15minEnabled;

    @Column(name = "reminder_1hour_enabled", nullable = false, columnDefinition = "TINYINT(1)")
    private boolean reminder1hourEnabled;

    @Column(name = "reminder_1day_enabled", nullable = false, columnDefinition = "TINYINT(1)")
    private boolean reminder1dayEnabled;

    @Column(name = "reminder_2days_enabled", nullable = false, columnDefinition = "TINYINT(1)")
    private boolean reminder2daysEnabled;

    @Column(name = "overdue_enabled", nullable = false, columnDefinition = "TINYINT(1)")
    private boolean overdueEnabled;
}
