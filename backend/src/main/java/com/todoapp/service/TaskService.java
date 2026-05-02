package com.todoapp.service;

import com.todoapp.dto.request.TaskCancelRequest;
import com.todoapp.dto.request.TaskCreateRequest;
import com.todoapp.dto.request.TaskUpdateRequest;
import com.todoapp.dto.response.TaskResponse;
import com.todoapp.entity.ModuleMember;
import com.todoapp.entity.Module;
import com.todoapp.entity.PermissionLevel;
import com.todoapp.entity.Tag;
import com.todoapp.entity.Task;
import com.todoapp.entity.TaskCompletion;
import com.todoapp.entity.TaskCompletionId;
import com.todoapp.entity.User;
import com.todoapp.exception.ForbiddenException;
import com.todoapp.exception.ResourceNotFoundException;
import com.todoapp.exception.ValidationException;
import com.todoapp.mapper.TaskMapper;
import com.todoapp.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service layer for task management operations.
 *
 * <p>Handles creation, retrieval, update, completion toggling, cancellation,
 * and soft-deletion of tasks, with permission checks enforced at each operation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {

    private final UserRepository userRepository;
    private final ModuleRepository moduleRepository;
    private final ModuleMemberRepository moduleMemberRepository;
    private final TaskRepository taskRepository;
    private final TaskCompletionRepository taskCompletionRepository;
    private final TagRepository tagRepository;
    private final TaskMapper taskMapper;

    // =========================================================================
    // Create
    // =========================================================================

    /**
     * Creates a new task in the specified module.
     *
     * @param request the task creation payload
     * @param userId  the ID of the authenticated user
     * @return the created task as a response DTO
     * @throws ResourceNotFoundException if the user or module is not found
     * @throws ForbiddenException        if the user lacks EDIT/ADMIN access to the module
     */
    @Transactional
    public TaskResponse createTask(TaskCreateRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        Module module = moduleRepository.findByIdAndDeletedAtIsNull(request.getModuleId())
                .orElseThrow(() -> new ResourceNotFoundException("Module", request.getModuleId()));

        if (!hasEditOrAdminAccess(module, userId)) {
            throw new ForbiddenException("You do not have permission to create tasks in this module");
        }

        Task.Priority priority = request.getPriority() != null
                ? request.getPriority()
                : Task.Priority.MEDIUM;

        Set<Tag> tags = resolveTags(request.getTags());

        Task task = Task.builder()
                .module(module)
                .createdBy(user)
                .title(request.getTitle())
                .description(request.getDescription())
                .status(Task.TaskStatus.PENDING)
                .priority(priority)
                .dueDate(request.getDueDate())
                .reminder15min(request.isReminder15min())
                .reminder1hour(request.isReminder1hour())
                .reminder1day(request.isReminder1day())
                .reminder2days(request.isReminder2days())
                .tags(tags)
                .build();

        Task savedTask = taskRepository.save(task);
        log.info("Created task id={} in module id={} by user id={}", savedTask.getId(), module.getId(), userId);
        return taskMapper.toResponse(savedTask, false, null);
    }

    // =========================================================================
    // Read
    // =========================================================================

    /**
     * Returns tasks accessible to the user, with optional filters.
     *
     * @param userId   the authenticated user's ID
     * @param moduleId optional module filter
     * @param status   optional status filter (matches {@link Task.TaskStatus} enum name)
     * @param priority optional priority filter (matches {@link Task.Priority} enum name)
     * @param tag      optional tag name filter
     * @return list of matching task response DTOs
     * @throws ForbiddenException if a specific moduleId is provided but not accessible
     */
    @Transactional(readOnly = true)
    public List<TaskResponse> getTasks(Long userId, Long moduleId, String status, String priority, String tag) {
        // Collect all module IDs the user can access
        List<Long> ownedModuleIds = moduleRepository.findByOwnerIdAndDeletedAtIsNull(userId)
                .stream()
                .map(Module::getId)
                .collect(Collectors.toList());

        List<Long> memberModuleIds = moduleMemberRepository.findByUserId(userId)
                .stream()
                .map(mm -> mm.getModule().getId())
                .collect(Collectors.toList());

        Set<Long> accessibleModuleIds = new HashSet<>();
        accessibleModuleIds.addAll(ownedModuleIds);
        accessibleModuleIds.addAll(memberModuleIds);

        List<Long> queryModuleIds;
        if (moduleId != null) {
            if (!accessibleModuleIds.contains(moduleId)) {
                throw new ForbiddenException("You do not have access to module with id: " + moduleId);
            }
            queryModuleIds = List.of(moduleId);
        } else {
            queryModuleIds = List.copyOf(accessibleModuleIds);
        }

        if (queryModuleIds.isEmpty()) {
            return List.of();
        }

        List<Task> tasks = taskRepository.findByModuleIdInAndDeletedAtIsNull(queryModuleIds);

        // Apply in-memory filters
        List<Task> filtered = tasks.stream()
                .filter(t -> {
                    if (status != null && !status.isBlank()) {
                        try {
                            Task.TaskStatus s = Task.TaskStatus.valueOf(status.toUpperCase());
                            return t.getStatus() == s;
                        } catch (IllegalArgumentException e) {
                            return false;
                        }
                    }
                    return true;
                })
                .filter(t -> {
                    if (priority != null && !priority.isBlank()) {
                        try {
                            Task.Priority p = Task.Priority.valueOf(priority.toUpperCase());
                            return t.getPriority() == p;
                        } catch (IllegalArgumentException e) {
                            return false;
                        }
                    }
                    return true;
                })
                .filter(t -> {
                    if (tag != null && !tag.isBlank()) {
                        return t.getTags() != null && t.getTags().stream()
                                .anyMatch(tg -> tg.getName().equalsIgnoreCase(tag));
                    }
                    return true;
                })
                .collect(Collectors.toList());

        // Bulk-fetch which tasks this user has personally completed (avoids N+1)
        List<Long> filteredIds = filtered.stream().map(Task::getId).collect(Collectors.toList());
        Set<Long> completedIds = filteredIds.isEmpty()
                ? Set.of()
                : taskCompletionRepository.findCompletedTaskIdsByUserIdAndTaskIdIn(userId, filteredIds);

        // If filtering by status=COMPLETED, keep only tasks the user personally completed
        // If filtering by status=PENDING, keep only tasks the user has NOT personally completed
        List<Task> statusFiltered = filtered;
        if (status != null && !status.isBlank()) {
            String upperStatus = status.toUpperCase();
            if ("COMPLETED".equals(upperStatus)) {
                statusFiltered = filtered.stream()
                        .filter(t -> completedIds.contains(t.getId()))
                        .collect(Collectors.toList());
            } else if ("PENDING".equals(upperStatus)) {
                statusFiltered = filtered.stream()
                        .filter(t -> t.getStatus() != Task.TaskStatus.CANCELLED
                                && !completedIds.contains(t.getId()))
                        .collect(Collectors.toList());
            }
        }

        // Build completion timestamp map for the response
        Map<Long, LocalDateTime> completionMap = new HashMap<>();
        if (!completedIds.isEmpty()) {
            taskCompletionRepository.findAllById(
                    completedIds.stream()
                            .map(tid -> new TaskCompletionId(tid, userId))
                            .collect(Collectors.toList())
            ).forEach(tc -> completionMap.put(tc.getTask().getId(), tc.getCompletedAt()));
        }

        return taskMapper.toResponseList(statusFiltered, completedIds, completionMap);
    }

    /**
     * Returns a single task by ID, verifying the user has access.
     *
     * @param taskId the task ID
     * @param userId the authenticated user's ID
     * @return the task response DTO
     * @throws ResourceNotFoundException if the task does not exist or is soft-deleted
     * @throws ForbiddenException        if the user has no access to the task's module
     */
    @Transactional(readOnly = true)
    public TaskResponse getTask(Long taskId, Long userId) {
        Task task = taskRepository.findByIdAndDeletedAtIsNull(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", taskId));

        if (!hasViewOrHigherAccess(task.getModule(), userId)) {
            throw new ForbiddenException("You do not have access to this task");
        }

        boolean completedByMe = taskCompletionRepository.existsByTaskIdAndUserId(task.getId(), userId);
        LocalDateTime completedAt = null;
        if (completedByMe) {
            completedAt = taskCompletionRepository.findById(new TaskCompletionId(task.getId(), userId))
                    .map(TaskCompletion::getCompletedAt).orElse(null);
        }
        return taskMapper.toResponse(task, completedByMe, completedAt);
    }

    // =========================================================================
    // Update
    // =========================================================================

    /**
     * Updates mutable fields of an existing task.
     *
     * @param taskId  the task ID
     * @param request the update payload (all fields optional)
     * @param userId  the authenticated user's ID
     * @return the updated task response DTO
     * @throws ResourceNotFoundException if the task does not exist or is soft-deleted
     * @throws ForbiddenException        if the user lacks EDIT/ADMIN access
     */
    @Transactional
    public TaskResponse updateTask(Long taskId, TaskUpdateRequest request, Long userId) {
        Task task = taskRepository.findByIdAndDeletedAtIsNull(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", taskId));

        if (!hasEditOrAdminAccess(task.getModule(), userId)) {
            throw new ForbiddenException("You do not have permission to update this task");
        }

        User editor = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        if (request.getTitle() != null) {
            task.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            task.setDescription(request.getDescription());
        }
        if (request.getPriority() != null) {
            task.setPriority(request.getPriority());
        }
        if (request.getDueDate() != null) {
            task.setDueDate(request.getDueDate());
        }
        if (request.getReminder15min() != null) {
            task.setReminder15min(request.getReminder15min());
        }
        if (request.getReminder1hour() != null) {
            task.setReminder1hour(request.getReminder1hour());
        }
        if (request.getReminder1day() != null) {
            task.setReminder1day(request.getReminder1day());
        }
        if (request.getReminder2days() != null) {
            task.setReminder2days(request.getReminder2days());
        }
        if (request.getTags() != null) {
            task.setTags(resolveTags(request.getTags()));
        }

        task.setLastEditedBy(editor);

        Task savedTask = taskRepository.save(task);
        log.info("Updated task id={} by user id={}", taskId, userId);
        boolean completedByMe = taskCompletionRepository.existsByTaskIdAndUserId(taskId, userId);
        LocalDateTime completedAt = null;
        if (completedByMe) {
            completedAt = taskCompletionRepository.findById(new TaskCompletionId(taskId, userId))
                    .map(TaskCompletion::getCompletedAt).orElse(null);
        }
        return taskMapper.toResponse(savedTask, completedByMe, completedAt);
    }

    // =========================================================================
    // Complete / Revert
    // =========================================================================

    /**
     * Toggles the requesting user's personal completion of a task.
     * - If not yet completed by this user: inserts a TaskCompletion row.
     * - If already completed by this user: deletes the row (revert).
     * The shared task status is NOT changed (it stays PENDING unless cancelled).
     *
     * @throws ValidationException if the task is CANCELLED
     */
    @Transactional
    public TaskResponse completeTask(Long taskId, Long userId) {
        Task task = taskRepository.findByIdAndDeletedAtIsNull(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", taskId));

        if (task.getStatus() == Task.TaskStatus.CANCELLED) {
            throw new ValidationException("Cannot complete a cancelled task");
        }

        if (!hasViewOrHigherAccess(task.getModule(), userId)) {
            throw new ForbiddenException("You do not have permission to complete this task");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        boolean alreadyCompleted = taskCompletionRepository.existsByTaskIdAndUserId(taskId, userId);

        LocalDateTime completedAt = null;
        boolean completedByMe;

        if (alreadyCompleted) {
            // Revert: remove the personal completion record
            taskCompletionRepository.deleteByTaskIdAndUserId(taskId, userId);
            completedByMe = false;
            log.info("Reverted personal completion for task id={} by user id={}", taskId, userId);
        } else {
            // Complete: insert a personal completion record
            TaskCompletion completion = TaskCompletion.builder()
                    .task(task)
                    .user(user)
                    .build();
            TaskCompletion saved = taskCompletionRepository.save(completion);
            completedAt = saved.getCompletedAt();
            completedByMe = true;
            log.info("Marked task id={} as personally completed by user id={}", taskId, userId);
        }

        task.setLastEditedBy(user);
        taskRepository.save(task);

        return taskMapper.toResponse(task, completedByMe, completedAt);
    }

    // =========================================================================
    // Cancel
    // =========================================================================

    /**
     * Cancels a task. Only the task creator or module owner may cancel.
     *
     * @param taskId  the task ID
     * @param request the cancel payload (optional cancellation reason)
     * @param userId  the authenticated user's ID
     * @return the updated task response DTO
     * @throws ResourceNotFoundException if the task does not exist or is soft-deleted
     * @throws ForbiddenException        if the user is neither creator nor module owner
     * @throws ValidationException       if the task is already cancelled or completed
     */
    @Transactional
    public TaskResponse cancelTask(Long taskId, TaskCancelRequest request, Long userId) {
        Task task = taskRepository.findByIdAndDeletedAtIsNull(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", taskId));

        boolean isCreator = task.getCreatedBy().getId().equals(userId);
        boolean isOwner = task.getModule().getOwner().getId().equals(userId);
        if (!isCreator && !isOwner) {
            throw new ForbiddenException("You do not have permission to cancel this task");
        }

        switch (task.getStatus()) {
            case CANCELLED -> throw new ValidationException("Task is already cancelled");
            case COMPLETED -> throw new ValidationException("Cannot cancel a completed task");
            default -> { /* PENDING — proceed */ }
        }

        User editor = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        task.setStatus(Task.TaskStatus.CANCELLED);
        task.setCancelledAt(LocalDateTime.now());
        task.setLastEditedBy(editor);
        if (request != null) {
            task.setCancellationReason(request.getCancellationReason());
        }

        Task savedTask = taskRepository.save(task);
        log.info("Cancelled task id={} by user id={}", taskId, userId);
        return taskMapper.toResponse(savedTask, false, null);
    }

    // =========================================================================
    // Delete (soft)
    // =========================================================================

    /**
     * Soft-deletes a task. Only the module owner or a member with ADMIN permission may delete.
     *
     * @param taskId the task ID
     * @param userId the authenticated user's ID
     * @throws ResourceNotFoundException if the task does not exist or is already soft-deleted
     * @throws ForbiddenException        if the user is not the module owner or an ADMIN member
     */
    @Transactional
    public void deleteTask(Long taskId, Long userId) {
        Task task = taskRepository.findByIdAndDeletedAtIsNull(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", taskId));

        Module module = task.getModule();
        boolean isOwner = module.getOwner().getId().equals(userId);
        boolean isAdmin = moduleMemberRepository.findByModuleIdAndUserId(module.getId(), userId)
                .map(mm -> mm.getPermissionLevel() == PermissionLevel.ADMIN)
                .orElse(false);

        if (!isOwner && !isAdmin) {
            throw new ForbiddenException("You do not have permission to delete this task");
        }

        task.setDeletedAt(LocalDateTime.now());
        taskRepository.save(task);
        log.info("Soft-deleted task id={} by user id={}", taskId, userId);
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    /**
     * Returns {@code true} if the user is the module owner or has EDIT/ADMIN permission.
     */
    private boolean hasEditOrAdminAccess(Module module, Long userId) {
        if (module.getOwner().getId().equals(userId)) {
            return true;
        }
        return moduleMemberRepository.findByModuleIdAndUserId(module.getId(), userId)
                .map(mm -> mm.getPermissionLevel() == PermissionLevel.EDIT
                        || mm.getPermissionLevel() == PermissionLevel.ADMIN)
                .orElse(false);
    }

    /**
     * Returns {@code true} if the user is the module owner or any member (VIEW, EDIT, or ADMIN).
     */
    private boolean hasViewOrHigherAccess(Module module, Long userId) {
        if (module.getOwner().getId().equals(userId)) {
            return true;
        }
        return moduleMemberRepository.findByModuleIdAndUserId(module.getId(), userId).isPresent();
    }

    /**
     * Resolves a list of tag names to {@link Tag} entities, creating any that do not yet exist.
     */
    private Set<Tag> resolveTags(List<String> tagNames) {
        Set<Tag> tags = new HashSet<>();
        if (tagNames == null || tagNames.isEmpty()) {
            return tags;
        }
        for (String name : tagNames) {
            if (name == null || name.isBlank()) {
                continue;
            }
            String trimmed = name.trim();
            Tag tag = tagRepository.findByName(trimmed)
                    .orElseGet(() -> tagRepository.save(Tag.builder().name(trimmed).build()));
            tags.add(tag);
        }
        return tags;
    }
}
