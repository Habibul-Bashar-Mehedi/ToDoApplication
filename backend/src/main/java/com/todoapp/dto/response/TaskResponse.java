package com.todoapp.dto.response;

import com.todoapp.entity.Task.Priority;
import com.todoapp.entity.Task.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskResponse {

    private Long id;
    private String title;
    private String description;
    private Long moduleId;
    private String moduleName;
    private Long createdBy;
    private String createdByDisplayName;
    private Long lastEditedBy;
    private String lastEditedByDisplayName;
    private TaskStatus status;
    private Priority priority;
    private LocalDateTime dueDate;
    private boolean reminder15min;
    private boolean reminder1hour;
    private boolean reminder1day;
    private boolean reminder2days;
    /** True when the requesting user has personally marked this task as done. */
    private boolean completedByMe;
    /** When the requesting user completed this task (null if not completed by them). */
    private LocalDateTime completedByMeAt;
    private LocalDateTime completedAt;
    private LocalDateTime cancelledAt;
    private String cancellationReason;
    private List<String> tags;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
