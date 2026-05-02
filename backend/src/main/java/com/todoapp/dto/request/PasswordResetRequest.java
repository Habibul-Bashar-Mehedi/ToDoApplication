package com.todoapp.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Used for both the forgot-password step (only email required)
 * and the reset-password step (token + newPassword required).
 */
@Data
public class PasswordResetRequest {

    @Email(message = "Email must be a valid address")
    private String email;

    private String token;

    @Size(min = 8, message = "Password must be at least 8 characters")
    @Pattern(
        regexp = "^(?=.*[a-zA-Z])(?=.*\\d).+$",
        message = "Password must contain at least one letter and one digit"
    )
    private String newPassword;
}
