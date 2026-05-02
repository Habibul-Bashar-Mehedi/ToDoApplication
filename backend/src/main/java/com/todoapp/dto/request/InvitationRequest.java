package com.todoapp.dto.request;

import com.todoapp.entity.PermissionLevel;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class InvitationRequest {

    @NotBlank(message = "Invitee email is required")
    @Email(message = "Invitee email must be a valid address")
    private String inviteeEmail;

    @NotNull(message = "Permission level is required")
    private PermissionLevel permissionLevel;
}
