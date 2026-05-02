package com.todoapp.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TaskCancelRequest {

    @Size(max = 500, message = "Cancellation reason must not exceed 500 characters")
    private String cancellationReason;
}
