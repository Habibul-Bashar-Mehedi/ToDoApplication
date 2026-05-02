package com.todoapp.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/**
 * Replaces the real {@link JavaMailSender} with a no-op stub during tests.
 *
 * <p>The stub is configured to point at a non-existent local SMTP server.
 * Because {@link com.todoapp.service.EmailService} catches and logs all send
 * failures, tests proceed normally even though no real email is delivered.
 */
@TestConfiguration
public class TestMailConfig {

    @Bean
    @Primary
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost("localhost");
        sender.setPort(3025);
        return sender;
    }
}
