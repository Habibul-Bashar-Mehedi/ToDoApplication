package com.todoapp.service;

import com.todoapp.dto.request.LoginRequest;
import com.todoapp.dto.request.PasswordResetRequest;
import com.todoapp.dto.request.RegisterRequest;
import com.todoapp.dto.response.AuthResponse;
import com.todoapp.entity.Module;
import com.todoapp.entity.User;
import com.todoapp.exception.ConflictException;
import com.todoapp.exception.ResourceNotFoundException;
import com.todoapp.exception.ValidationException;
import com.todoapp.repository.ModuleRepository;
import com.todoapp.repository.UserRepository;
import com.todoapp.security.JwtTokenProvider;
import com.todoapp.security.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Core authentication service.
 *
 * <p>Handles user registration, email verification, login (with lockout logic),
 * logout (JWT blacklisting), and password reset.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    /** Number of consecutive failed logins before the account is locked. */
    private static final int MAX_FAILED_ATTEMPTS = 5;

    /** Duration (minutes) after which a locked account is automatically unlocked. */
    private static final long LOCKOUT_DURATION_MINUTES = 30L;

    /** Validity window (hours) for email verification tokens. */
    private static final long VERIFICATION_TOKEN_EXPIRY_HOURS = 24L;

    /** Validity window (hours) for password-reset tokens. */
    private static final long RESET_TOKEN_EXPIRY_HOURS = 1L;

    private final UserRepository userRepository;
    private final ModuleRepository moduleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlacklistService tokenBlacklistService;
    private final EmailService emailService;

    /** When false (dev mode), skip email verification — auto-verify on registration. */
    @org.springframework.beans.factory.annotation.Value("${app.mail.enabled:true}")
    private boolean mailEnabled;

    // =========================================================================
    // Registration
    // =========================================================================

    /**
     * Registers a new user account.
     *
     * <ol>
     *   <li>Validates email uniqueness.</li>
     *   <li>Hashes the password with BCrypt (strength 12, configured in {@code SecurityConfig}).</li>
     *   <li>Creates the user with {@code email_verified = false}.</li>
     *   <li>Generates a UUID v4 email-verification token (24-hour expiry).</li>
     *   <li>Creates a default "Personal Tasks" module owned by the new user.</li>
     *   <li>Sends the verification email asynchronously.</li>
     * </ol>
     *
     * @param request the registration payload
     * @throws ConflictException if the email is already registered
     */
    @Transactional
    public void register(RegisterRequest request) {
        String email = request.getEmail().toLowerCase().trim();

        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("An account with this email address already exists");
        }

        String verificationToken = UUID.randomUUID().toString();

        // In dev mode (mail disabled), auto-verify so users can log in immediately
        boolean autoVerify = !mailEnabled;

        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .displayName(request.getDisplayName().trim())
                .emailVerified(autoVerify)
                .emailVerificationToken(autoVerify ? null : verificationToken)
                .emailVerificationExpiresAt(autoVerify ? null :
                        LocalDateTime.now().plusHours(VERIFICATION_TOKEN_EXPIRY_HOURS))
                .failedLoginAttempts(0)
                .build();

        user = userRepository.save(user);
        log.info("Registered new user id={} email={} (autoVerified={})", user.getId(), email, autoVerify);

        // Create the default "Personal Tasks" module for the new user
        Module personalModule = Module.builder()
                .owner(user)
                .name("Personal Tasks")
                .visibility(Module.Visibility.PRIVATE)
                .build();
        moduleRepository.save(personalModule);
        log.debug("Created default 'Personal Tasks' module for user id={}", user.getId());

        // Send verification email only when mail is enabled
        if (!autoVerify) {
            emailService.sendVerificationEmail(email, verificationToken);
        }
    }

    // =========================================================================
    // Email verification
    // =========================================================================

    /**
     * Verifies a user's email address using the token from the verification link.
     *
     * @param token the UUID v4 verification token
     * @throws ValidationException       if the token is invalid or has expired
     * @throws ResourceNotFoundException if no user is associated with the token
     */
    @Transactional
    public void verifyEmail(String token) {
        User user = userRepository.findByEmailVerificationToken(token)
                .orElseThrow(() -> new ValidationException(
                        "Invalid or expired email verification token"));

        if (user.getEmailVerificationExpiresAt() == null
                || LocalDateTime.now().isAfter(user.getEmailVerificationExpiresAt())) {
            throw new ValidationException(
                    "Email verification token has expired. Please request a new verification email.");
        }

        user.setEmailVerified(true);
        user.setEmailVerificationToken(null);
        user.setEmailVerificationExpiresAt(null);
        userRepository.save(user);
        log.info("Email verified for user id={}", user.getId());
    }

    /**
     * Resends the email verification link to the user.
     *
     * <p>Generates a fresh token and expiry. If the account is already verified,
     * this is a no-op (returns silently to avoid leaking account existence).
     *
     * @param email the user's email address
     */
    @Transactional
    public void resendVerification(String email) {
        String normalizedEmail = email.toLowerCase().trim();
        userRepository.findByEmail(normalizedEmail).ifPresent(user -> {
            if (!user.isEmailVerified()) {
                String newToken = UUID.randomUUID().toString();
                user.setEmailVerificationToken(newToken);
                user.setEmailVerificationExpiresAt(
                        LocalDateTime.now().plusHours(VERIFICATION_TOKEN_EXPIRY_HOURS));
                userRepository.save(user);
                emailService.sendVerificationEmail(normalizedEmail, newToken);
                log.debug("Resent verification email to {}", normalizedEmail);
            }
        });
    }

    // =========================================================================
    // Login
    // =========================================================================

    /**
     * Authenticates a user and issues a JWT.
     *
     * <p>Login flow:
     * <ol>
     *   <li>Look up the user by email (generic error if not found — do not reveal existence).</li>
     *   <li>Check email verification status.</li>
     *   <li>Check account lockout; auto-unlock if 30 minutes have elapsed.</li>
     *   <li>Validate the password; increment failed-attempt counter on failure.</li>
     *   <li>Lock the account after {@value #MAX_FAILED_ATTEMPTS} consecutive failures.</li>
     *   <li>On success, reset the failed-attempt counter and issue a JWT.</li>
     * </ol>
     *
     * @param request the login payload
     * @return an {@link AuthResponse} containing the JWT and user info
     * @throws ValidationException if credentials are invalid, account is unverified, or locked
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail().toLowerCase().trim();

        // Use a generic error message to avoid revealing whether the email exists
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ValidationException("Invalid email or password"));

        // Check email verification
        if (!user.isEmailVerified()) {
            throw new ValidationException(
                    "Your email address has not been verified. "
                    + "Please check your inbox or request a new verification email.");
        }

        // Auto-unlock if lockout duration has elapsed
        if (user.getLockedAt() != null) {
            LocalDateTime unlockTime = user.getLockedAt().plusMinutes(LOCKOUT_DURATION_MINUTES);
            if (LocalDateTime.now().isAfter(unlockTime)) {
                user.setLockedAt(null);
                user.setFailedLoginAttempts(0);
                userRepository.save(user);
                log.info("Auto-unlocked account for user id={}", user.getId());
            } else {
                throw new ValidationException(
                        "Your account has been temporarily locked due to too many failed login attempts. "
                        + "Please try again in " + LOCKOUT_DURATION_MINUTES + " minutes.");
            }
        }

        // Validate password
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);

            if (attempts >= MAX_FAILED_ATTEMPTS) {
                user.setLockedAt(LocalDateTime.now());
                userRepository.save(user);
                log.warn("Account locked for user id={} after {} failed attempts",
                        user.getId(), attempts);
                throw new ValidationException(
                        "Your account has been locked after " + MAX_FAILED_ATTEMPTS
                        + " failed login attempts. Please try again in "
                        + LOCKOUT_DURATION_MINUTES + " minutes.");
            }

            userRepository.save(user);
            log.debug("Failed login attempt {} for user id={}", attempts, user.getId());
            throw new ValidationException("Invalid email or password");
        }

        // Successful login — reset failed attempts
        user.setFailedLoginAttempts(0);
        user.setLockedAt(null);
        userRepository.save(user);

        // Build UserDetails for token generation
        UserDetails userDetails = buildUserDetails(user);
        String token = jwtTokenProvider.generateToken(userDetails, user.getId(), request.isRememberMe());
        long expiresIn = jwtTokenProvider.getExpiryMillis(token) - System.currentTimeMillis();

        log.info("User id={} logged in (rememberMe={})", user.getId(), request.isRememberMe());

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(expiresIn)
                .user(AuthResponse.UserInfo.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .displayName(user.getDisplayName())
                        .emailVerified(user.isEmailVerified())
                        .build())
                .build();
    }

    // =========================================================================
    // Logout
    // =========================================================================

    /**
     * Invalidates the current JWT by adding its {@code jti} to the blacklist.
     *
     * @param token the raw JWT string (without the "Bearer " prefix)
     */
    public void logout(String token) {
        try {
            String jti = jwtTokenProvider.getJtiFromToken(token);
            long expiryMillis = jwtTokenProvider.getExpiryMillis(token);
            tokenBlacklistService.blacklist(jti, expiryMillis);
            log.debug("Blacklisted token jti={}", jti);
        } catch (Exception e) {
            // Token may already be invalid/expired — log and ignore
            log.debug("Could not blacklist token during logout: {}", e.getMessage());
        }
    }

    // =========================================================================
    // Password reset
    // =========================================================================

    /**
     * Initiates the password-reset flow by generating a reset token and sending
     * the reset email.
     *
     * <p>If no account exists for the given email, this method returns silently
     * to avoid leaking account existence.
     *
     * @param email the user's email address
     */
    @Transactional
    public void forgotPassword(String email) {
        String normalizedEmail = email.toLowerCase().trim();
        userRepository.findByEmail(normalizedEmail).ifPresent(user -> {
            String resetToken = UUID.randomUUID().toString();
            user.setPasswordResetToken(resetToken);
            user.setPasswordResetExpiresAt(LocalDateTime.now().plusHours(RESET_TOKEN_EXPIRY_HOURS));
            userRepository.save(user);
            emailService.sendPasswordResetEmail(normalizedEmail, resetToken);
            log.debug("Password reset email sent to {}", normalizedEmail);
        });
    }

    /**
     * Completes the password-reset flow: validates the token, hashes the new
     * password, persists it, and invalidates the token.
     *
     * @param request the reset payload containing the token and new password
     * @throws ValidationException if the token is invalid or has expired
     */
    @Transactional
    public void resetPassword(PasswordResetRequest request) {
        if (request.getToken() == null || request.getToken().isBlank()) {
            throw new ValidationException("Reset token is required");
        }
        if (request.getNewPassword() == null || request.getNewPassword().isBlank()) {
            throw new ValidationException("New password is required");
        }

        User user = userRepository.findByPasswordResetToken(request.getToken())
                .orElseThrow(() -> new ValidationException(
                        "Invalid or expired password reset token"));

        if (user.getPasswordResetExpiresAt() == null
                || LocalDateTime.now().isAfter(user.getPasswordResetExpiresAt())) {
            throw new ValidationException(
                    "Password reset token has expired. Please request a new password reset.");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordResetToken(null);
        user.setPasswordResetExpiresAt(null);
        userRepository.save(user);
        log.info("Password reset completed for user id={}", user.getId());
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private UserDetails buildUserDetails(User user) {
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPasswordHash())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_USER")))
                .build();
    }
}
