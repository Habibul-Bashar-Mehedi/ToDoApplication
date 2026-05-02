package com.todoapp.service;

import com.todoapp.dto.response.InvitationResponse;
import com.todoapp.entity.Invitation;
import com.todoapp.entity.InvitationStatus;
import com.todoapp.entity.ModuleMember;
import com.todoapp.exception.ForbiddenException;
import com.todoapp.exception.ResourceNotFoundException;
import com.todoapp.mapper.ModuleMapper;
import com.todoapp.repository.InvitationRepository;
import com.todoapp.repository.ModuleMemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service layer for invitation management.
 *
 * <p>Handles listing, accepting, and rejecting module invitations for the
 * authenticated user.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InvitationService {

    private final InvitationRepository invitationRepository;
    private final ModuleMemberRepository moduleMemberRepository;
    private final ModuleMapper moduleMapper;

    // =========================================================================
    // List
    // =========================================================================

    /**
     * Returns all pending invitations for the authenticated user.
     *
     * @param userId the authenticated user's ID
     * @return list of pending invitation response DTOs
     */
    @Transactional(readOnly = true)
    public List<InvitationResponse> getInvitations(Long userId) {
        List<Invitation> invitations = invitationRepository
                .findByInviteeIdAndStatus(userId, InvitationStatus.PENDING);
        return moduleMapper.toInvitationResponseList(invitations);
    }

    // =========================================================================
    // Accept
    // =========================================================================

    /**
     * Accepts a pending invitation.
     *
     * <ul>
     *   <li>Verifies the invitation belongs to the authenticated user.</li>
     *   <li>Creates a {@link ModuleMember} record with the invitation's permission level.</li>
     *   <li>Marks the invitation as {@link InvitationStatus#ACCEPTED}.</li>
     * </ul>
     *
     * @param invitationId the invitation ID
     * @param userId       the authenticated user's ID
     * @return the updated invitation response DTO
     * @throws ResourceNotFoundException if the invitation is not found
     * @throws ForbiddenException        if the invitation does not belong to the user
     * @throws IllegalStateException     if the invitation is not in PENDING status
     */
    @Transactional
    public InvitationResponse acceptInvitation(Long invitationId, Long userId) {
        Invitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation", invitationId));

        if (!invitation.getInvitee().getId().equals(userId)) {
            throw new ForbiddenException("You can only accept your own invitations");
        }

        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new IllegalStateException(
                    "Invitation is already " + invitation.getStatus().name().toLowerCase());
        }

        // Create the ModuleMember record
        ModuleMember member = ModuleMember.builder()
                .module(invitation.getModule())
                .user(invitation.getInvitee())
                .permissionLevel(invitation.getPermissionLevel())
                .build();
        moduleMemberRepository.save(member);

        // Mark invitation as accepted
        invitation.setStatus(InvitationStatus.ACCEPTED);
        invitation.setRespondedAt(LocalDateTime.now());
        Invitation saved = invitationRepository.save(invitation);

        log.info("Invitation id={} accepted by user id={} for module id={}",
                invitationId, userId, invitation.getModule().getId());
        return moduleMapper.toInvitationResponse(saved);
    }

    // =========================================================================
    // Reject
    // =========================================================================

    /**
     * Rejects a pending invitation.
     *
     * <ul>
     *   <li>Verifies the invitation belongs to the authenticated user.</li>
     *   <li>Marks the invitation as {@link InvitationStatus#REJECTED}.</li>
     * </ul>
     *
     * @param invitationId the invitation ID
     * @param userId       the authenticated user's ID
     * @return the updated invitation response DTO
     * @throws ResourceNotFoundException if the invitation is not found
     * @throws ForbiddenException        if the invitation does not belong to the user
     * @throws IllegalStateException     if the invitation is not in PENDING status
     */
    @Transactional
    public InvitationResponse rejectInvitation(Long invitationId, Long userId) {
        Invitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation", invitationId));

        if (!invitation.getInvitee().getId().equals(userId)) {
            throw new ForbiddenException("You can only reject your own invitations");
        }

        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new IllegalStateException(
                    "Invitation is already " + invitation.getStatus().name().toLowerCase());
        }

        invitation.setStatus(InvitationStatus.REJECTED);
        invitation.setRespondedAt(LocalDateTime.now());
        Invitation saved = invitationRepository.save(invitation);

        log.info("Invitation id={} rejected by user id={}", invitationId, userId);
        return moduleMapper.toInvitationResponse(saved);
    }
}
