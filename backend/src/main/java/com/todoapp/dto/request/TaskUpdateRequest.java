package com.todoapp.dto.request;

import com.todoapp.entity.Task.Priority;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class TaskUpdateRequest {

    @Size(min = 1, max = 200, message = "Title must be between 1 and 200 characters")
    private String title;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    private Priority priority;

    @Future(message = "Due date must be in the future")
    private LocalDateTime dueDate;

    private Boolean reminder15min;
    private Boolean reminder1hour;
    private Boolean reminder1day;
    private Boolean reminder2days;

    private List<String> tags;
}
