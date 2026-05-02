package com.todoapp.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.todoapp.entity.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {

    private Long id;
    private Long userId;
    private Long taskId;
    private NotificationType type;
    private String message;

    /**
     * Serialized as "isRead" (not "read") so the frontend model field name matches.
     * Named "read" internally to avoid Lombok generating isIsRead() for a boolean
     * field named "isRead". @JsonProperty overrides the serialized name.
     */
    @JsonProperty("isRead")
    private boolean read;

    private LocalDateTime createdAt;
}
