package com.todoapp.dto.response;

import com.todoapp.entity.InvitationStatus;
import com.todoapp.entity.PermissionLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvitationResponse {

    private Long id;
    private Long moduleId;
    private String moduleName;
    private Long inviterId;
    private String inviterDisplayName;
    private Long inviteeId;
    private String inviteeEmail;
    private PermissionLevel permissionLevel;
    private InvitationStatus status;
    private LocalDateTime invitedAt;
    private LocalDateTime respondedAt;
}
