package com.todoapp.service;

import com.todoapp.dto.request.InvitationRequest;
import com.todoapp.dto.request.ModuleCreateRequest;
import com.todoapp.dto.request.ModuleUpdateRequest;
import com.todoapp.dto.response.InvitationResponse;
import com.todoapp.dto.response.ModuleMemberResponse;
import com.todoapp.dto.response.ModuleResponse;
import com.todoapp.entity.Invitation;
import com.todoapp.entity.InvitationStatus;
import com.todoapp.entity.Module;
import com.todoapp.entity.ModuleMember;
import com.todoapp.entity.NotificationType;
import com.todoapp.entity.PermissionLevel;
import com.todoapp.entity.Task;
import com.todoapp.entity.User;
import com.todoapp.exception.ConflictException;
import com.todoapp.exception.ForbiddenException;
import com.todoapp.exception.ResourceNotFoundException;
import com.todoapp.exception.ValidationException;
import com.todoapp.mapper.ModuleMapper;
import com.todoapp.repository.InvitationRepository;
import com.todoapp.repository.ModuleMemberRepository;
import com.todoapp.repository.ModuleRepository;
import com.todoapp.repository.TaskRepository;
import com.todoapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service layer for module management operations.
 *
 * <p>Handles creation, retrieval, update, and soft-deletion of modules,
 * as well as member management (invite, update permission, remove) and
 * in-app notifications for invitations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModuleService {

    private final UserRepository userRepository;
    private final ModuleRepository moduleRepository;
    private final ModuleMemberRepository moduleMemberRepository;
    private final TaskRepository taskRepository;
    private final InvitationRepository invitationRepository;
    private final ModuleMapper moduleMapper;
    private final NotificationService notificationService;

    // =========================================================================
    // Create
    // =========================================================================

    /**
     * Creates a new module owned by the authenticated user.
     *
     * <ul>
     *   <li>Enforces name uniqueness per owner (non-deleted modules).</li>
     *   <li>Defaults visibility to {@link Module.Visibility#PRIVATE} if not provided.</li>
     *   <li>Assigns the authenticated user as the owner.</li>
     * </ul>
     *
     * @param request the module creation payload
     * @param userId  the ID of the authenticated user
     * @return the created module as a response DTO
     * @throws ResourceNotFoundException if the user is not found
     * @throws ConflictException         if a non-deleted module with the same name already exists for this owner
     */
    @Transactional
    public ModuleResponse createModule(ModuleCreateRequest request, Long userId) {
        User owner = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        if (moduleRepository.existsByOwnerIdAndNameAndDeletedAtIsNull(userId, request.getName())) {
            throw new ConflictException(
                    "A module named '" + request.getName() + "' already exists for this account");
        }

        Module.Visibility visibility = request.getVisibility() != null
                ? request.getVisibility()
                : Module.Visibility.PRIVATE;

        Module module = Module.builder()
                .owner(owner)
                .name(request.getName())
                .description(request.getDescription())
                .visibility(visibility)
                .build();

        Module saved = moduleRepository.save(module);
        log.info("Created module id={} name='{}' for user id={}", saved.getId(), saved.getName(), userId);
        return moduleMapper.toResponse(saved);
    }

    // =========================================================================
    // Read
    // =========================================================================

    /**
     * Returns all non-deleted modules accessible to the user:
     * modules the user owns plus modules where the user is a member.
     *
     * @param userId the authenticated user's ID
     * @return list of accessible module response DTOs
     */
    @Transactional(readOnly = true)
    public List<ModuleResponse> getModules(Long userId) {
        List<Module> ownedModules = moduleRepository.findByOwnerIdAndDeletedAtIsNull(userId);

        List<Module> memberModules = moduleMemberRepository.findByUserId(userId)
                .stream()
                .map(ModuleMember::getModule)
                .filter(m -> m.getDeletedAt() == null)
                .collect(Collectors.toList());

        // Merge, deduplicating by ID
        Set<Long> seen = new HashSet<>();
        List<Module> allModules = new ArrayList<>();
        for (Module m : ownedModules) {
            if (seen.add(m.getId())) {
                allModules.add(m);
            }
        }
        for (Module m : memberModules) {
            if (seen.add(m.getId())) {
                allModules.add(m);
            }
        }

        return moduleMapper.toResponseList(allModules);
    }

    /**
     * Returns a single non-deleted module by ID.
     * The requesting user must be the owner or a member.
     *
     * @param moduleId the module ID
     * @param userId   the authenticated user's ID
     * @return the module response DTO
     * @throws ResourceNotFoundException if the module does not exist or is soft-deleted
     * @throws ForbiddenException        if the user is neither owner nor member
     */
    @Transactional(readOnly = true)
    public ModuleResponse getModule(Long moduleId, Long userId) {
        Module module = moduleRepository.findByIdAndDeletedAtIsNull(moduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Module", moduleId));

        if (!isOwnerOrMember(module, userId)) {
            throw new ForbiddenException("You do not have access to this module");
        }

        return moduleMapper.toResponse(module);
    }

    // =========================================================================
    // Update
    // =========================================================================

    /**
     * Updates mutable fields of an existing module.
     * Only the module owner may perform this operation.
     * If the name is changed, uniqueness is re-validated.
     *
     * @param moduleId the module ID
     * @param request  the update payload (all fields optional)
     * @param userId   the authenticated user's ID
     * @return the updated module response DTO
     * @throws ResourceNotFoundException if the module does not exist or is soft-deleted
     * @throws ForbiddenException        if the user is not the module owner
     * @throws ConflictException         if the new name conflicts with an existing module for this owner
     */
    @Transactional
    public ModuleResponse updateModule(Long moduleId, ModuleUpdateRequest request, Long userId) {
        Module module = moduleRepository.findByIdAndDeletedAtIsNull(moduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Module", moduleId));

        if (!module.getOwner().getId().equals(userId)) {
            throw new ForbiddenException("Only the module owner can update this module");
        }

        if (request.getName() != null && !request.getName().equals(module.getName())) {
            if (moduleRepository.existsByOwnerIdAndNameAndDeletedAtIsNull(userId, request.getName())) {
                throw new ConflictException(
                        "A module named '" + request.getName() + "' already exists for this account");
            }
            module.setName(request.getName());
        }

        if (request.getDescription() != null) {
            module.setDescription(request.getDescription());
        }

        if (request.getVisibility() != null) {
            module.setVisibility(request.getVisibility());
        }

        Module saved = moduleRepository.save(module);
        log.info("Updated module id={} by user id={}", moduleId, userId);
        return moduleMapper.toResponse(saved);
    }

    // =========================================================================
    // Delete (soft)
    // =========================================================================

    /**
     * Soft-deletes a module and cascades the soft-delete to all its tasks.
     * Only the module owner may perform this operation.
     *
     * @param moduleId the module ID
     * @param userId   the authenticated user's ID
     * @throws ResourceNotFoundException if the module does not exist or is already soft-deleted
     * @throws ForbiddenException        if the user is not the module owner
     */
    @Transactional
    public void deleteModule(Long moduleId, Long userId) {
        Module module = moduleRepository.findByIdAndDeletedAtIsNull(moduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Module", moduleId));

        if (!module.getOwner().getId().equals(userId)) {
            throw new ForbiddenException("Only the module owner can delete this module");
        }

        LocalDateTime now = LocalDateTime.now();

        // Cascade soft-delete to all non-deleted tasks in this module
        List<Task> tasks = taskRepository.findByModuleIdAndDeletedAtIsNull(moduleId);
        for (Task task : tasks) {
            task.setDeletedAt(now);
        }
        taskRepository.saveAll(tasks);

        module.setDeletedAt(now);
        moduleRepository.save(module);

        log.info("Soft-deleted module id={} and {} tasks by user id={}", moduleId, tasks.size(), userId);
    }

    // =========================================================================
    // Members
    // =========================================================================

    /**
     * Returns the list of members for a module.
     * The requesting user must be the owner or a member.
     *
     * @param moduleId the module ID
     * @param userId   the authenticated user's ID
     * @return list of module member response DTOs
     * @throws ResourceNotFoundException if the module does not exist or is soft-deleted
     * @throws ForbiddenException        if the user is neither owner nor member
     */
    @Transactional(readOnly = true)
    public List<ModuleMemberResponse> getMembers(Long moduleId, Long userId) {
        Module module = moduleRepository.findByIdAndDeletedAtIsNull(moduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Module", moduleId));

        if (!isOwnerOrMember(module, userId)) {
            throw new ForbiddenException("You do not have access to this module");
        }

        List<ModuleMember> members = moduleMemberRepository.findByModuleId(moduleId);
        return moduleMapper.toMemberResponseList(members);
    }

    /**
     * Invites a user to a module by email.
     *
     * <ul>
     *   <li>The requesting user must be the owner or an ADMIN member.</li>
     *   <li>The invitee must exist and have a verified email address.</li>
     *   <li>Creates an {@link Invitation} with {@link InvitationStatus#PENDING} status.</li>
     *   <li>Sends an in-app {@link NotificationType#INVITATION} notification to the invitee.</li>
     * </ul>
     *
     * @param moduleId  the module ID
     * @param request   the invitation payload (invitee email + permission level)
     * @param userId    the ID of the requesting user
     * @return the created invitation as a response DTO
     * @throws ResourceNotFoundException if the module or invitee is not found
     * @throws ForbiddenException        if the requester lacks owner/ADMIN permission
     * @throws ValidationException       if the invitee's email is not verified
     * @throws ConflictException         if a pending invitation already exists for this user/module
     */
    @Transactional
    public InvitationResponse inviteMember(Long moduleId, InvitationRequest request, Long userId) {
        Module module = moduleRepository.findByIdAndDeletedAtIsNull(moduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Module", moduleId));

        if (!isOwnerOrAdmin(module, userId)) {
            throw new ForbiddenException("Only the module owner or an ADMIN member can invite users");
        }

        User inviter = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        User invitee = userRepository.findByEmail(request.getInviteeEmail())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User", "email", request.getInviteeEmail()));

        if (!invitee.isEmailVerified()) {
            throw new ValidationException(
                    "Cannot invite a user whose email address has not been verified");
        }

        // Prevent duplicate pending invitations
        if (invitationRepository.existsByModuleIdAndInviteeIdAndStatus(
                moduleId, invitee.getId(), InvitationStatus.PENDING)) {
            throw new ConflictException(
                    "A pending invitation already exists for this user in this module");
        }

        Invitation invitation = Invitation.builder()
                .module(module)
                .inviter(inviter)
                .invitee(invitee)
                .permissionLevel(request.getPermissionLevel())
                .status(InvitationStatus.PENDING)
                .build();

        Invitation saved = invitationRepository.save(invitation);
        log.info("Created invitation id={} for module id={} invitee id={} by user id={}",
                saved.getId(), moduleId, invitee.getId(), userId);

        // Send in-app notification to the invitee
        String message = inviter.getDisplayName() + " has invited you to join the module '"
                + module.getName() + "' with " + request.getPermissionLevel().name() + " permission.";
        notificationService.createNotification(invitee, null, NotificationType.INVITATION, message);

        return moduleMapper.toInvitationResponse(saved);
    }

    /**
     * Updates the permission level of an existing module member.
     * The requesting user must be the owner or an ADMIN member.
     * The owner's permission cannot be changed via this method.
     *
     * @param moduleId         the module ID
     * @param targetUserId     the ID of the member whose permission is being updated
     * @param newLevel         the new permission level
     * @param requestingUserId the ID of the user making the request
     * @return the updated member response DTO
     * @throws ResourceNotFoundException if the module or target member is not found
     * @throws ForbiddenException        if the requester lacks owner/ADMIN permission,
     *                                   or if attempting to change the owner's permission
     */
    @Transactional
    public ModuleMemberResponse updateMemberPermission(Long moduleId, Long targetUserId,
                                                        PermissionLevel newLevel,
                                                        Long requestingUserId) {
        Module module = moduleRepository.findByIdAndDeletedAtIsNull(moduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Module", moduleId));

        if (!isOwnerOrAdmin(module, requestingUserId)) {
            throw new ForbiddenException(
                    "Only the module owner or an ADMIN member can update member permissions");
        }

        // Cannot change the owner's permission
        if (module.getOwner().getId().equals(targetUserId)) {
            throw new ForbiddenException("Cannot change the permission level of the module owner");
        }

        ModuleMember member = moduleMemberRepository.findByModuleIdAndUserId(moduleId, targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "ModuleMember", "userId", targetUserId));

        member.setPermissionLevel(newLevel);
        ModuleMember saved = moduleMemberRepository.save(member);
        log.info("Updated permission for user id={} in module id={} to {} by user id={}",
                targetUserId, moduleId, newLevel, requestingUserId);
        return moduleMapper.toMemberResponse(saved);
    }

    /**
     * Removes a member from a module.
     * The requesting user must be the owner or an ADMIN member.
     * The module owner cannot be removed.
     *
     * @param moduleId         the module ID
     * @param targetUserId     the ID of the member to remove
     * @param requestingUserId the ID of the user making the request
     * @throws ResourceNotFoundException if the module or target member is not found
     * @throws ForbiddenException        if the requester lacks owner/ADMIN permission,
     *                                   or if attempting to remove the module owner
     */
    @Transactional
    public void removeMember(Long moduleId, Long targetUserId, Long requestingUserId) {
        Module module = moduleRepository.findByIdAndDeletedAtIsNull(moduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Module", moduleId));

        if (!isOwnerOrAdmin(module, requestingUserId)) {
            throw new ForbiddenException(
                    "Only the module owner or an ADMIN member can remove members");
        }

        // Cannot remove the module owner
        if (module.getOwner().getId().equals(targetUserId)) {
            throw new ForbiddenException("Cannot remove the module owner from the module");
        }

        ModuleMember member = moduleMemberRepository.findByModuleIdAndUserId(moduleId, targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "ModuleMember", "userId", targetUserId));

        moduleMemberRepository.delete(member);
        log.info("Removed user id={} from module id={} by user id={}",
                targetUserId, moduleId, requestingUserId);
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    /**
     * Returns {@code true} if the user is the module owner or any member (VIEW, EDIT, or ADMIN).
     */
    private boolean isOwnerOrMember(Module module, Long userId) {
        if (module.getOwner().getId().equals(userId)) {
            return true;
        }
        return moduleMemberRepository.findByModuleIdAndUserId(module.getId(), userId).isPresent();
    }

    /**
     * Returns {@code true} if the user is the module owner or has ADMIN permission.
     */
    private boolean isOwnerOrAdmin(Module module, Long userId) {
        if (module.getOwner().getId().equals(userId)) {
            return true;
        }
        return moduleMemberRepository.findByModuleIdAndUserId(module.getId(), userId)
                .map(mm -> mm.getPermissionLevel() == PermissionLevel.ADMIN)
                .orElse(false);
    }
}
