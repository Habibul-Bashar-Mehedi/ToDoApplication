package com.todoapp.dto.response;

import com.todoapp.entity.Module.Visibility;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModuleResponse {

    private Long id;
    private Long ownerId;
    private String ownerDisplayName;
    private String name;
    private String description;
    private Visibility visibility;
    private int taskCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
