package com.todoapp.mapper;

import com.todoapp.dto.response.InvitationResponse;
import com.todoapp.dto.response.ModuleMemberResponse;
import com.todoapp.dto.response.ModuleResponse;
import com.todoapp.entity.Invitation;
import com.todoapp.entity.Module;
import com.todoapp.entity.ModuleMember;
import com.todoapp.entity.Task;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class ModuleMapper {

    private final ModelMapper modelMapper;

    public ModuleMapper(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    /**
     * Maps a {@link Module} entity to a {@link ModuleResponse} DTO.
     * Counts only non-deleted tasks for the taskCount field.
     */
    public ModuleResponse toResponse(Module module) {
        ModuleResponse response = new ModuleResponse();
        response.setId(module.getId());
        response.setName(module.getName());
        response.setDescription(module.getDescription());
        response.setVisibility(module.getVisibility());
        response.setCreatedAt(module.getCreatedAt());
        response.setUpdatedAt(module.getUpdatedAt());

        // Flatten owner association
        if (module.getOwner() != null) {
            response.setOwnerId(module.getOwner().getId());
            response.setOwnerDisplayName(module.getOwner().getDisplayName());
        }

        // Count non-deleted tasks
        if (module.getTasks() != null) {
            long count = module.getTasks().stream()
                    .filter(t -> t.getDeletedAt() == null)
                    .count();
            response.setTaskCount((int) count);
        }

        return response;
    }

    /**
     * Maps a list of {@link Module} entities to a list of {@link ModuleResponse} DTOs.
     */
    public List<ModuleResponse> toResponseList(List<Module> modules) {
        return modules.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Maps a {@link ModuleMember} entity to a {@link ModuleMemberResponse} DTO.
     */
    public ModuleMemberResponse toMemberResponse(ModuleMember member) {
        ModuleMemberResponse response = new ModuleMemberResponse();
        response.setId(member.getId());
        response.setPermissionLevel(member.getPermissionLevel());
        response.setCreatedAt(member.getCreatedAt());

        if (member.getModule() != null) {
            response.setModuleId(member.getModule().getId());
        }

        if (member.getUser() != null) {
            response.setUserId(member.getUser().getId());
            response.setUserEmail(member.getUser().getEmail());
            response.setUserDisplayName(member.getUser().getDisplayName());
        }

        return response;
    }

    /**
     * Maps a list of {@link ModuleMember} entities to a list of {@link ModuleMemberResponse} DTOs.
     */
    public List<ModuleMemberResponse> toMemberResponseList(List<ModuleMember> members) {
        return members.stream()
                .map(this::toMemberResponse)
                .collect(Collectors.toList());
    }

    /**
     * Maps an {@link Invitation} entity to an {@link InvitationResponse} DTO.
     */
    public InvitationResponse toInvitationResponse(Invitation invitation) {
        InvitationResponse response = new InvitationResponse();
        response.setId(invitation.getId());
        response.setPermissionLevel(invitation.getPermissionLevel());
        response.setStatus(invitation.getStatus());
        response.setInvitedAt(invitation.getInvitedAt());
        response.setRespondedAt(invitation.getRespondedAt());

        if (invitation.getModule() != null) {
            response.setModuleId(invitation.getModule().getId());
            response.setModuleName(invitation.getModule().getName());
        }

        if (invitation.getInviter() != null) {
            response.setInviterId(invitation.getInviter().getId());
            response.setInviterDisplayName(invitation.getInviter().getDisplayName());
        }

        if (invitation.getInvitee() != null) {
            response.setInviteeId(invitation.getInvitee().getId());
            response.setInviteeEmail(invitation.getInvitee().getEmail());
        }

        return response;
    }

    /**
     * Maps a list of {@link Invitation} entities to a list of {@link InvitationResponse} DTOs.
     */
    public List<InvitationResponse> toInvitationResponseList(List<Invitation> invitations) {
        return invitations.stream()
                .map(this::toInvitationResponse)
                .collect(Collectors.toList());
    }
}
