package com.todoapp.dto.request;

import com.todoapp.entity.Task.Priority;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class TaskCreateRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    @NotNull(message = "Module ID is required")
    private Long moduleId;

    private Priority priority;

    @Future(message = "Due date must be in the future")
    private LocalDateTime dueDate;

    private boolean reminder15min;
    private boolean reminder1hour;
    private boolean reminder1day;
    private boolean reminder2days;

    private List<String> tags;
}
