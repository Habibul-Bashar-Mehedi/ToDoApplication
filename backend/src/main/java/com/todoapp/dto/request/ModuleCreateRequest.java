package com.todoapp.dto.request;

import com.todoapp.entity.Module.Visibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ModuleCreateRequest {

    @NotBlank(message = "Module name is required")
    @Size(max = 100, message = "Module name must not exceed 100 characters")
    private String name;

    private String description;

    private Visibility visibility;
}
