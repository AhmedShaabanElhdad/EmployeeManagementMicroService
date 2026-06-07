package com.example.notification.consumer;

import com.example.notification.service.EmailService;
import com.example.shared.events.PayrollInvoiceEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class PayrollInvoiceConsumer {

    private final EmailService emailService;

    @KafkaListener(topics = "payroll-invoice-topic", groupId = "notification-group")
    public void consume(PayrollInvoiceEvent event) {
        log.info("Received payroll invoice event for employee: {} for month: {}", 
                event.employeeId(), event.month());

        try {
            emailService.sendPayrollInvoiceMessage(event.email(), event.month(), event.netAmount());
            log.info("Payroll invoice email sent to {}", event.email());
        } catch (Exception e) {
            log.error("Failed to process payroll invoice notification for {}", event.email(), e);
        }
    }
}
