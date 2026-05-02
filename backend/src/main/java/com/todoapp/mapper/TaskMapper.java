package com.todoapp.mapper;

import com.todoapp.dto.response.TaskResponse;
import com.todoapp.entity.Tag;
import com.todoapp.entity.Task;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class TaskMapper {

    private final ModelMapper modelMapper;

    public TaskMapper(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    /**
     * Maps a {@link Task} entity to a {@link TaskResponse} DTO.
     * Flattens nested associations (module, createdBy, tags) into scalar fields.
     *
     * @param task          the task entity
     * @param completedByMe whether the requesting user has personally completed this task
     * @param completedAt   when the requesting user completed this task (null if not)
     */
    public TaskResponse toResponse(Task task, boolean completedByMe, java.time.LocalDateTime completedAt) {
        TaskResponse response = new TaskResponse();
        response.setId(task.getId());
        response.setTitle(task.getTitle());
        response.setDescription(task.getDescription());
        response.setStatus(task.getStatus());
        response.setPriority(task.getPriority());
        response.setDueDate(task.getDueDate());
        response.setReminder15min(task.isReminder15min());
        response.setReminder1hour(task.isReminder1hour());
        response.setReminder1day(task.isReminder1day());
        response.setReminder2days(task.isReminder2days());
        response.setCompletedByMe(completedByMe);
        response.setCompletedByMeAt(completedAt);
        response.setCompletedAt(task.getCompletedAt());
        response.setCancelledAt(task.getCancelledAt());
        response.setCancellationReason(task.getCancellationReason());
        response.setCreatedAt(task.getCreatedAt());
        response.setUpdatedAt(task.getUpdatedAt());

        // Flatten module association
        if (task.getModule() != null) {
            response.setModuleId(task.getModule().getId());
            response.setModuleName(task.getModule().getName());
        }

        // Flatten createdBy association
        if (task.getCreatedBy() != null) {
            response.setCreatedBy(task.getCreatedBy().getId());
            response.setCreatedByDisplayName(task.getCreatedBy().getDisplayName());
        }

        // Flatten lastEditedBy association
        if (task.getLastEditedBy() != null) {
            response.setLastEditedBy(task.getLastEditedBy().getId());
            response.setLastEditedByDisplayName(task.getLastEditedBy().getDisplayName());
        }

        // Flatten tags to list of name strings
        if (task.getTags() != null) {
            List<String> tagNames = task.getTags().stream()
                    .map(Tag::getName)
                    .sorted()
                    .collect(Collectors.toList());
            response.setTags(tagNames);
        } else {
            response.setTags(Collections.emptyList());
        }

        return response;
    }

    /**
     * Convenience overload — completedByMe defaults to false (used for single-task lookups
     * where the caller will set the flag separately, or for non-user-scoped responses).
     */
    public TaskResponse toResponse(Task task) {
        return toResponse(task, false, null);
    }

    /**
     * Maps a list of tasks, bulk-annotating each with the requesting user's completion status.
     *
     * @param tasks         the task entities
     * @param completedIds  set of task IDs the requesting user has personally completed
     * @param completionMap map of task ID → completion timestamp for the requesting user
     */
    public List<TaskResponse> toResponseList(
            List<Task> tasks,
            java.util.Set<Long> completedIds,
            java.util.Map<Long, java.time.LocalDateTime> completionMap) {
        return tasks.stream()
                .map(t -> toResponse(t, completedIds.contains(t.getId()), completionMap.get(t.getId())))
                .collect(Collectors.toList());
    }

    /**
     * Maps a list of {@link Task} entities to a list of {@link TaskResponse} DTOs
     * without per-user completion data (all completedByMe = false).
     */
    public List<TaskResponse> toResponseList(List<Task> tasks) {
        return tasks.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
}
