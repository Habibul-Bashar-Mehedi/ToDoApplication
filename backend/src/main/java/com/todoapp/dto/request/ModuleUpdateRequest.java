package com.todoapp.dto.request;

import com.todoapp.entity.Module.Visibility;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ModuleUpdateRequest {

    @Size(min = 1, max = 100, message = "Module name must be between 1 and 100 characters")
    private String name;

    private String description;

    private Visibility visibility;
}
