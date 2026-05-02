package com.todoapp.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreferenceResponse {

    private Long id;
    private Long userId;
    private boolean reminder15minEnabled;
    private boolean reminder1hourEnabled;
    private boolean reminder1dayEnabled;
    private boolean reminder2daysEnabled;
    private boolean overdueEnabled;
}
