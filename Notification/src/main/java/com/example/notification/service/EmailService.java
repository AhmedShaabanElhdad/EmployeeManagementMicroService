package com.example.notification.service;

import com.example.shared.monitoring.MetricsProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final MetricsProvider metricsProvider;

    @Value("${backend.origin}")
    private String origin;

    @Value("${GEMAI_APP_USERNAME}")
    private String from;

    @Retryable(
            retryFor = Exception.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 3000)
    )
    public void sendMessage(String to, String token) {
        long startTime = System.currentTimeMillis();
        metricsProvider.incrementCounter("notification.email.attempt", "to", to);
        
        // Constructing the signup URL pointing to the API Gateway
        String url = origin + "/api/v1/auth/signup?token=" + token;
        
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setSubject("Employee Management System - Complete Your Registration");
        message.setTo(to);
        message.setText("Welcome! \n\nYour employee profile has been created. Please complete your registration by clicking the link below:\n\n" + url + "\n\nIf you did not expect this email, please ignore it.");
        
        try {
            mailSender.send(message);
            metricsProvider.recordExecutionTime("notification.email.send.time", System.currentTimeMillis() - startTime);
            metricsProvider.incrementCounter("notification.email.success", "to", to);
            log.info("Successfully sent registration email to {}", to);
        } catch (Exception e) {
            metricsProvider.incrementCounter("notification.email.error", "reason", e.getClass().getSimpleName());
            log.error("Failed to send email to {}. Retrying...", to);
            throw e; // Throwing to trigger @Retryable
        }
    }
}
