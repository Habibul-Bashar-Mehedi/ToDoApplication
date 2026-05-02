package com.todoapp.mapper;

import com.todoapp.dto.response.NotificationPreferenceResponse;
import com.todoapp.dto.response.NotificationResponse;
import com.todoapp.entity.Notification;
import com.todoapp.entity.NotificationPreference;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class NotificationMapper {

    private final ModelMapper modelMapper;

    public NotificationMapper(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    /**
     * Maps a {@link Notification} entity to a {@link NotificationResponse} DTO.
     */
    public NotificationResponse toResponse(Notification notification) {
        NotificationResponse response = new NotificationResponse();
        response.setId(notification.getId());
        response.setType(notification.getType());
        response.setMessage(notification.getMessage());
        response.setRead(notification.isRead());
        response.setCreatedAt(notification.getCreatedAt());

        if (notification.getUser() != null) {
            response.setUserId(notification.getUser().getId());
        }

        if (notification.getTask() != null) {
            response.setTaskId(notification.getTask().getId());
        }

        return response;
    }

    /**
     * Maps a list of {@link Notification} entities to a list of {@link NotificationResponse} DTOs.
     */
    public List<NotificationResponse> toResponseList(List<Notification> notifications) {
        return notifications.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Maps a {@link NotificationPreference} entity to a {@link NotificationPreferenceResponse} DTO.
     */
    public NotificationPreferenceResponse toPreferenceResponse(NotificationPreference preference) {
        NotificationPreferenceResponse response = new NotificationPreferenceResponse();
        response.setId(preference.getId());
        response.setReminder15minEnabled(preference.isReminder15minEnabled());
        response.setReminder1hourEnabled(preference.isReminder1hourEnabled());
        response.setReminder1dayEnabled(preference.isReminder1dayEnabled());
        response.setReminder2daysEnabled(preference.isReminder2daysEnabled());
        response.setOverdueEnabled(preference.isOverdueEnabled());

        if (preference.getUser() != null) {
            response.setUserId(preference.getUser().getId());
        }

        return response;
    }
}
