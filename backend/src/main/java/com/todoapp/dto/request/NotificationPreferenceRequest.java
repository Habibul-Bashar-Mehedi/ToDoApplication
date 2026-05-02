package com.todoapp.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class NotificationPreferenceRequest {

    @NotNull(message = "reminder15minEnabled is required")
    private Boolean reminder15minEnabled;

    @NotNull(message = "reminder1hourEnabled is required")
    private Boolean reminder1hourEnabled;

    @NotNull(message = "reminder1dayEnabled is required")
    private Boolean reminder1dayEnabled;

    @NotNull(message = "reminder2daysEnabled is required")
    private Boolean reminder2daysEnabled;

    @NotNull(message = "overdueEnabled is required")
    private Boolean overdueEnabled;
}
