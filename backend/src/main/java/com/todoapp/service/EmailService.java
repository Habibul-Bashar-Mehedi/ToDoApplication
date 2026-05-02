package com.todoapp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Sends transactional emails for email verification and password reset.
 *
 * <p>All send methods are {@code @Async} so they do not block the request thread.
 * In the test profile the {@link JavaMailSender} bean is replaced with a no-op
 * stub (configured in {@code application-test.yml}) so no real emails are sent
 * during tests.
 *
 * <p>Set {@code app.mail.enabled=false} (default in dev profile) to skip SMTP
 * entirely and log email content to the console instead — useful for copying
 * verification/reset links during local development.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:noreply@todoapp.local}")
    private String fromAddress;

    @Value("${app.base-url:http://localhost:4200}")
    private String baseUrl;

    /** When false, emails are logged to console instead of sent via SMTP. */
    @Value("${app.mail.enabled:true}")
    private boolean mailEnabled;

    /**
     * Sends an email verification link to the newly registered user.
     *
     * @param toEmail           the recipient's email address
     * @param verificationToken the UUID v4 token to embed in the link
     */
    @Async
    public void sendVerificationEmail(String toEmail, String verificationToken) {
        String link = baseUrl + "/verify-email?token=" + verificationToken;
        String subject = "Verify your To-Do App account";
        String body = "Hello,\n\n"
                + "Thank you for registering. Please verify your email address by clicking the link below:\n\n"
                + link + "\n\n"
                + "This link expires in 24 hours.\n\n"
                + "If you did not create an account, you can safely ignore this email.\n\n"
                + "— The To-Do App Team";

        sendEmail(toEmail, subject, body);
    }

    /**
     * Sends a password-reset link to the user.
     *
     * @param toEmail    the recipient's email address
     * @param resetToken the UUID v4 token to embed in the link
     */
    @Async
    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        String link = baseUrl + "/reset-password?token=" + resetToken;
        String subject = "Reset your To-Do App password";
        String body = "Hello,\n\n"
                + "We received a request to reset the password for your account.\n\n"
                + "Click the link below to choose a new password:\n\n"
                + link + "\n\n"
                + "This link expires in 1 hour.\n\n"
                + "If you did not request a password reset, you can safely ignore this email.\n\n"
                + "— The To-Do App Team";

        sendEmail(toEmail, subject, body);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void sendEmail(String to, String subject, String body) {
        if (!mailEnabled) {
            // Dev mode: log the full email so links can be copied from the console
            log.info("\n=== [DEV] Email not sent (app.mail.enabled=false) ==="
                    + "\nTo:      {}"
                    + "\nSubject: {}"
                    + "\nBody:\n{}"
                    + "\n=====================================================",
                    to, subject, body);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.debug("Email sent to {} with subject '{}'", to, subject);
        } catch (Exception e) {
            // Log but do not propagate — a failed email must not roll back the
            // database transaction that already committed the user record.
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }
}
