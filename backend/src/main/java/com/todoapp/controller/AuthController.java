package com.todoapp.controller;

import com.todoapp.dto.request.LoginRequest;
import com.todoapp.dto.request.PasswordResetRequest;
import com.todoapp.dto.request.RegisterRequest;
import com.todoapp.dto.response.AuthResponse;
import com.todoapp.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for all authentication endpoints.
 *
 * <p>Base path: {@code /auth} (relative to the global context path {@code /api/v1}).
 * All endpoints are publicly accessible (no JWT required) except {@code /auth/logout},
 * which requires a valid Bearer token.
 */
@Tag(name = "Authentication", description = "User registration, login, logout, and password management")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthService authService;

    // =========================================================================
    // POST /auth/register
    // =========================================================================

    @Operation(
        summary = "Register a new user account",
        description = "Creates a new user account and sends an email verification link. "
                    + "The account cannot be used until the email is verified."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Account created; verification email sent"),
        @ApiResponse(responseCode = "400", description = "Validation failure (invalid email, weak password, etc.)"),
        @ApiResponse(responseCode = "409", description = "Email address already registered")
    })
    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(
            @Valid @RequestBody RegisterRequest request) {

        authService.register(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(Map.of("message",
                        "Registration successful. Please check your email to verify your account."));
    }

    // =========================================================================
    // POST /auth/verify-email
    // =========================================================================

    @Operation(
        summary = "Verify email address",
        description = "Activates the user account using the token from the verification email."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Email verified successfully"),
        @ApiResponse(responseCode = "400", description = "Token is invalid or has expired")
    })
    @PostMapping("/verify-email")
    public ResponseEntity<Map<String, String>> verifyEmail(
            @RequestParam @NotBlank(message = "Token is required") String token) {

        authService.verifyEmail(token);
        return ResponseEntity.ok(Map.of("message", "Email verified successfully. You can now log in."));
    }

    // =========================================================================
    // POST /auth/resend-verification
    // =========================================================================

    @Operation(
        summary = "Resend email verification link",
        description = "Generates a new verification token and resends the verification email. "
                    + "Returns 200 regardless of whether the email exists to avoid leaking account information."
    )
    @ApiResponse(responseCode = "200", description = "If the email is registered and unverified, a new link has been sent")
    @PostMapping("/resend-verification")
    public ResponseEntity<Map<String, String>> resendVerification(
            @RequestParam @Email(message = "A valid email address is required") String email) {

        authService.resendVerification(email);
        return ResponseEntity.ok(Map.of("message",
                "If your email address is registered and unverified, a new verification link has been sent."));
    }

    // =========================================================================
    // POST /auth/login
    // =========================================================================

    @Operation(
        summary = "Log in and receive a JWT",
        description = "Authenticates the user and returns a signed JWT. "
                    + "Use the 'rememberMe' flag to request a 7-day token instead of the default 30-minute token."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Login successful; JWT returned"),
        @ApiResponse(responseCode = "400", description = "Invalid credentials, unverified account, or account locked")
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request) {

        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // POST /auth/logout
    // =========================================================================

    @Operation(
        summary = "Log out and invalidate the current JWT",
        description = "Adds the current JWT's jti to the blacklist so it cannot be reused. "
                    + "Requires a valid Bearer token in the Authorization header."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Logged out successfully"),
        @ApiResponse(responseCode = "401", description = "No valid JWT provided")
    })
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        if (StringUtils.hasText(authHeader) && authHeader.startsWith(BEARER_PREFIX)) {
            String token = authHeader.substring(BEARER_PREFIX.length());
            authService.logout(token);
        }
        return ResponseEntity.ok(Map.of("message", "Logged out successfully."));
    }

    // =========================================================================
    // POST /auth/forgot-password
    // =========================================================================

    @Operation(
        summary = "Request a password reset email",
        description = "Sends a password-reset link to the registered email address. "
                    + "Always returns 200 to avoid leaking account existence."
    )
    @ApiResponse(responseCode = "200", description = "If the email is registered, a reset link has been sent")
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(
            @RequestParam @Email(message = "A valid email address is required") String email) {

        authService.forgotPassword(email);
        return ResponseEntity.ok(Map.of("message",
                "If your email address is registered, a password reset link has been sent."));
    }

    // =========================================================================
    // POST /auth/reset-password
    // =========================================================================

    @Operation(
        summary = "Reset password using a reset token",
        description = "Validates the reset token, hashes the new password, and invalidates the token. "
                    + "The 'token' and 'newPassword' fields are required."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Password reset successfully"),
        @ApiResponse(responseCode = "400", description = "Token is invalid, expired, or new password fails validation")
    })
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(
            @Valid @RequestBody PasswordResetRequest request) {

        authService.resetPassword(request);
        return ResponseEntity.ok(Map.of("message",
                "Password reset successfully. You can now log in with your new password."));
    }
}
