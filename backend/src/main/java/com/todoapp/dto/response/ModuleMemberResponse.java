package com.todoapp.dto.response;

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
public class ModuleMemberResponse {

    private Long id;
    private Long moduleId;
    private Long userId;
    private String userEmail;
    private String userDisplayName;
    private PermissionLevel permissionLevel;
    private LocalDateTime createdAt;
}
