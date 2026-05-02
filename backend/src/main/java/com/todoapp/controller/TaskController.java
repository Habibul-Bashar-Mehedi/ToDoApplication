package com.todoapp.controller;

import com.todoapp.dto.request.TaskCancelRequest;
import com.todoapp.dto.request.TaskCreateRequest;
import com.todoapp.dto.request.TaskUpdateRequest;
import com.todoapp.dto.response.TaskResponse;
import com.todoapp.exception.ResourceNotFoundException;
import com.todoapp.repository.UserRepository;
import com.todoapp.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for task management endpoints.
 *
 * <p>Base path: {@code /tasks} (relative to the global context path {@code /api/v1}).
 * All endpoints require a valid JWT (enforced by {@code SecurityConfig}).
 */
@Tag(name = "Tasks", description = "Create, retrieve, update, complete, cancel, and delete tasks")
@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;
    private final UserRepository userRepository;

    // =========================================================================
    // GET /tasks
    // =========================================================================

    @Operation(
        summary = "List tasks",
        description = "Returns all tasks accessible to the authenticated user. "
                    + "Optionally filter by moduleId, status, priority, or tag."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Tasks retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied to the specified module")
    })
    @GetMapping
    public ResponseEntity<List<TaskResponse>> getTasks(
            @RequestParam(required = false) Long moduleId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String tag,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = getCurrentUserId(userDetails);
        List<TaskResponse> tasks = taskService.getTasks(userId, moduleId, status, priority, tag);
        return ResponseEntity.ok(tasks);
    }

    // =========================================================================
    // POST /tasks
    // =========================================================================

    @Operation(
        summary = "Create a task",
        description = "Creates a new task in the specified module. "
                    + "The authenticated user must have EDIT or ADMIN access to the module."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Task created successfully"),
        @ApiResponse(responseCode = "400", description = "Validation failure"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions for the module"),
        @ApiResponse(responseCode = "404", description = "Module not found")
    })
    @PostMapping
    public ResponseEntity<TaskResponse> createTask(
            @Valid @RequestBody TaskCreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = getCurrentUserId(userDetails);
        TaskResponse response = taskService.createTask(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // =========================================================================
    // GET /tasks/{id}
    // =========================================================================

    @Operation(
        summary = "Get a task by ID",
        description = "Returns a single task. The authenticated user must have at least VIEW access to the task's module."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Task retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "Task not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<TaskResponse> getTask(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = getCurrentUserId(userDetails);
        TaskResponse response = taskService.getTask(id, userId);
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // PUT /tasks/{id}
    // =========================================================================

    @Operation(
        summary = "Update a task",
        description = "Updates mutable fields of an existing task. "
                    + "The authenticated user must have EDIT or ADMIN access to the task's module."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Task updated successfully"),
        @ApiResponse(responseCode = "400", description = "Validation failure"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
        @ApiResponse(responseCode = "404", description = "Task not found")
    })
    @PutMapping("/{id}")
    public ResponseEntity<TaskResponse> updateTask(
            @PathVariable Long id,
            @Valid @RequestBody TaskUpdateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = getCurrentUserId(userDetails);
        TaskResponse response = taskService.updateTask(id, request, userId);
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // PATCH /tasks/{id}/complete
    // =========================================================================

    @Operation(
        summary = "Toggle task completion",
        description = "Marks a PENDING task as COMPLETED, or reverts a COMPLETED task back to PENDING. "
                    + "Throws 400 if the task is CANCELLED."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Task completion toggled successfully"),
        @ApiResponse(responseCode = "400", description = "Task is cancelled and cannot be completed"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
        @ApiResponse(responseCode = "404", description = "Task not found")
    })
    @PatchMapping("/{id}/complete")
    public ResponseEntity<TaskResponse> completeTask(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = getCurrentUserId(userDetails);
        TaskResponse response = taskService.completeTask(id, userId);
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // PATCH /tasks/{id}/cancel
    // =========================================================================

    @Operation(
        summary = "Cancel a task",
        description = "Cancels a PENDING task. Only the task creator or module owner may cancel. "
                    + "An optional cancellation reason may be provided."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Task cancelled successfully"),
        @ApiResponse(responseCode = "400", description = "Task is already cancelled or completed"),
        @ApiResponse(responseCode = "403", description = "Only the task creator or module owner can cancel"),
        @ApiResponse(responseCode = "404", description = "Task not found")
    })
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<TaskResponse> cancelTask(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) TaskCancelRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = getCurrentUserId(userDetails);
        TaskResponse response = taskService.cancelTask(id, request, userId);
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // DELETE /tasks/{id}
    // =========================================================================

    @Operation(
        summary = "Delete a task",
        description = "Soft-deletes a task. Only the module owner or a member with ADMIN permission may delete."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Task deleted successfully"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
        @ApiResponse(responseCode = "404", description = "Task not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = getCurrentUserId(userDetails);
        taskService.deleteTask(id, userId);
        return ResponseEntity.noContent().build();
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    /**
     * Resolves the current user's database ID from their {@link UserDetails} (email as username).
     */
    private Long getCurrentUserId(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userDetails.getUsername()))
                .getId();
    }
}
