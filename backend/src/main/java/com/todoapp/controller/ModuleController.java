package com.todoapp.controller;

import com.todoapp.dto.request.InvitationRequest;
import com.todoapp.dto.request.ModuleCreateRequest;
import com.todoapp.dto.request.ModuleUpdateRequest;
import com.todoapp.dto.response.InvitationResponse;
import com.todoapp.dto.response.ModuleMemberResponse;
import com.todoapp.dto.response.ModuleResponse;
import com.todoapp.entity.PermissionLevel;
import com.todoapp.exception.ResourceNotFoundException;
import com.todoapp.repository.UserRepository;
import com.todoapp.service.InvitationService;
import com.todoapp.service.ModuleService;
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
 * REST controller for module and invitation management endpoints.
 *
 * <p>Base path: {@code /modules} and {@code /invitations}
 * (relative to the global context path {@code /api/v1}).
 * All endpoints require a valid JWT (enforced by {@code SecurityConfig}).
 */
@Tag(name = "Modules", description = "Create, retrieve, update, delete modules and manage members and invitations")
@RestController
@RequiredArgsConstructor
public class ModuleController {

    private final ModuleService moduleService;
    private final InvitationService invitationService;
    private final UserRepository userRepository;

    // =========================================================================
    // GET /modules
    // =========================================================================

    @Operation(
        summary = "List modules",
        description = "Returns all modules owned by or shared with the authenticated user."
    )
    @ApiResponse(responseCode = "200", description = "Modules retrieved successfully")
    @GetMapping("/modules")
    public ResponseEntity<List<ModuleResponse>> getModules(
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = getCurrentUserId(userDetails);
        return ResponseEntity.ok(moduleService.getModules(userId));
    }

    // =========================================================================
    // POST /modules
    // =========================================================================

    @Operation(
        summary = "Create a module",
        description = "Creates a new module owned by the authenticated user."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Module created successfully"),
        @ApiResponse(responseCode = "400", description = "Validation failure"),
        @ApiResponse(responseCode = "409", description = "Module name already exists for this user")
    })
    @PostMapping("/modules")
    public ResponseEntity<ModuleResponse> createModule(
            @Valid @RequestBody ModuleCreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = getCurrentUserId(userDetails);
        ModuleResponse response = moduleService.createModule(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // =========================================================================
    // GET /modules/{id}
    // =========================================================================

    @Operation(
        summary = "Get a module by ID",
        description = "Returns a single module. The user must be the owner or a member."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Module retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "Module not found")
    })
    @GetMapping("/modules/{id}")
    public ResponseEntity<ModuleResponse> getModule(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = getCurrentUserId(userDetails);
        return ResponseEntity.ok(moduleService.getModule(id, userId));
    }

    // =========================================================================
    // PUT /modules/{id}
    // =========================================================================

    @Operation(
        summary = "Update a module",
        description = "Updates the name, description, or visibility of a module. Only the owner may update."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Module updated successfully"),
        @ApiResponse(responseCode = "400", description = "Validation failure"),
        @ApiResponse(responseCode = "403", description = "Only the module owner can update"),
        @ApiResponse(responseCode = "404", description = "Module not found"),
        @ApiResponse(responseCode = "409", description = "New name conflicts with an existing module")
    })
    @PutMapping("/modules/{id}")
    public ResponseEntity<ModuleResponse> updateModule(
            @PathVariable Long id,
            @Valid @RequestBody ModuleUpdateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = getCurrentUserId(userDetails);
        return ResponseEntity.ok(moduleService.updateModule(id, request, userId));
    }

    // =========================================================================
    // DELETE /modules/{id}
    // =========================================================================

    @Operation(
        summary = "Delete a module",
        description = "Soft-deletes a module and all its tasks. Only the owner may delete."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Module deleted successfully"),
        @ApiResponse(responseCode = "403", description = "Only the module owner can delete"),
        @ApiResponse(responseCode = "404", description = "Module not found")
    })
    @DeleteMapping("/modules/{id}")
    public ResponseEntity<Void> deleteModule(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = getCurrentUserId(userDetails);
        moduleService.deleteModule(id, userId);
        return ResponseEntity.noContent().build();
    }

    // =========================================================================
    // GET /modules/{id}/members
    // =========================================================================

    @Operation(
        summary = "List module members",
        description = "Returns all members of a module. The user must be the owner or a member."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Members retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "Module not found")
    })
    @GetMapping("/modules/{id}/members")
    public ResponseEntity<List<ModuleMemberResponse>> getMembers(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = getCurrentUserId(userDetails);
        return ResponseEntity.ok(moduleService.getMembers(id, userId));
    }

    // =========================================================================
    // POST /modules/{id}/invitations
    // =========================================================================

    @Operation(
        summary = "Invite a user to a module",
        description = "Sends an invitation to a registered user by email. "
                    + "The requester must be the owner or an ADMIN member."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Invitation created successfully"),
        @ApiResponse(responseCode = "400", description = "Validation failure or invitee email not verified"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
        @ApiResponse(responseCode = "404", description = "Module or invitee not found"),
        @ApiResponse(responseCode = "409", description = "Pending invitation already exists for this user")
    })
    @PostMapping("/modules/{id}/invitations")
    public ResponseEntity<InvitationResponse> inviteMember(
            @PathVariable Long id,
            @Valid @RequestBody InvitationRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = getCurrentUserId(userDetails);
        InvitationResponse response = moduleService.inviteMember(id, request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // =========================================================================
    // PUT /modules/{id}/members/{userId}
    // =========================================================================

    @Operation(
        summary = "Update member permission",
        description = "Changes the permission level of an existing module member. "
                    + "The requester must be the owner or an ADMIN member."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Permission updated successfully"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions or attempting to change owner"),
        @ApiResponse(responseCode = "404", description = "Module or member not found")
    })
    @PutMapping("/modules/{id}/members/{memberId}")
    public ResponseEntity<ModuleMemberResponse> updateMemberPermission(
            @PathVariable Long id,
            @PathVariable Long memberId,
            @RequestParam PermissionLevel permissionLevel,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = getCurrentUserId(userDetails);
        ModuleMemberResponse response = moduleService.updateMemberPermission(
                id, memberId, permissionLevel, userId);
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // DELETE /modules/{id}/members/{userId}
    // =========================================================================

    @Operation(
        summary = "Remove a module member",
        description = "Revokes a user's access to a module. "
                    + "The requester must be the owner or an ADMIN member. The owner cannot be removed."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Member removed successfully"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions or attempting to remove owner"),
        @ApiResponse(responseCode = "404", description = "Module or member not found")
    })
    @DeleteMapping("/modules/{id}/members/{memberId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable Long id,
            @PathVariable Long memberId,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = getCurrentUserId(userDetails);
        moduleService.removeMember(id, memberId, userId);
        return ResponseEntity.noContent().build();
    }

    // =========================================================================
    // GET /invitations
    // =========================================================================

    @Operation(
        summary = "List pending invitations",
        description = "Returns all pending invitations for the authenticated user."
    )
    @ApiResponse(responseCode = "200", description = "Invitations retrieved successfully")
    @GetMapping("/invitations")
    public ResponseEntity<List<InvitationResponse>> getInvitations(
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = getCurrentUserId(userDetails);
        return ResponseEntity.ok(invitationService.getInvitations(userId));
    }

    // =========================================================================
    // PATCH /invitations/{id}/accept
    // =========================================================================

    @Operation(
        summary = "Accept an invitation",
        description = "Accepts a pending invitation, creating a ModuleMember record."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Invitation accepted successfully"),
        @ApiResponse(responseCode = "403", description = "Invitation does not belong to the user"),
        @ApiResponse(responseCode = "404", description = "Invitation not found")
    })
    @PatchMapping("/invitations/{id}/accept")
    public ResponseEntity<InvitationResponse> acceptInvitation(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = getCurrentUserId(userDetails);
        return ResponseEntity.ok(invitationService.acceptInvitation(id, userId));
    }

    // =========================================================================
    // PATCH /invitations/{id}/reject
    // =========================================================================

    @Operation(
        summary = "Reject an invitation",
        description = "Rejects a pending invitation."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Invitation rejected successfully"),
        @ApiResponse(responseCode = "403", description = "Invitation does not belong to the user"),
        @ApiResponse(responseCode = "404", description = "Invitation not found")
    })
    @PatchMapping("/invitations/{id}/reject")
    public ResponseEntity<InvitationResponse> rejectInvitation(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = getCurrentUserId(userDetails);
        return ResponseEntity.ok(invitationService.rejectInvitation(id, userId));
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private Long getCurrentUserId(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User", "email", userDetails.getUsername()))
                .getId();
    }
}
