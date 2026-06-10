package com.example.notification.consumer;

import com.example.notification.config.RabbitMQConfig;
import com.example.notification.dto.EmployeeCreatedEvent;
import com.example.notification.service.EmailService;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class EmployeeCreatedConsumer {

    private final EmailService emailService;

    @RabbitListener(
            queues = RabbitMQConfig.QUEUE
    )
    public void consume(EmployeeCreatedEvent event) {
        log.info(
                "Received employee creation event for {}",
                event.email()
        );

        emailService.sendRegistrationMessage(event.email(), event.token());
    }
}
