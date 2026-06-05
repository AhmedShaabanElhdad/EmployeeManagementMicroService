package com.example.employeeservice.service;

import com.example.employeeservice.dtos.EmployeeCreatedEvent;
import com.example.employeeservice.entity.Outbox;
import com.example.employeeservice.message.publisher.EmployeeEventPublisher;
import com.example.employeeservice.repo.OutboxRepo;
import com.example.shared.events.EmployeeSagaEvent;
import com.example.shared.monitoring.MetricsProvider;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxProcessor {

    private final OutboxRepo outboxRepo;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final EmployeeEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private final MetricsProvider metricsProvider;

    @Scheduled(fixedDelay = 5000) // Poll every 5 seconds
    @Transactional
    public void processOutboxEvents() {
        List<Outbox> unprocessedEvents = outboxRepo.findByProcessedFalse();

        // Record the current size of pending events (Gauge)
        metricsProvider.recordMetric("outbox.pending.size", unprocessedEvents.size());

        for (Outbox event : unprocessedEvents) {
            try {
                if ("EmployeeCreated".equals(event.getEventType())) {
                    EmployeeCreatedEvent payload = objectMapper.readValue(event.getPayload(), EmployeeCreatedEvent.class);
                    eventPublisher.publishEmployeeCreated(payload);
                } else if ("EmployeeSagaStart".equals(event.getEventType())) {
                    EmployeeSagaEvent sagaPayload = objectMapper.readValue(event.getPayload(), EmployeeSagaEvent.class);

                    // todo add .whenComplete
                    kafkaTemplate.send("employee-saga-topic", sagaPayload);
                }

                event.setProcessed(true);
                event.setProcessedAt(Instant.now());
                outboxRepo.save(event);

                log.info("Successfully processed outbox event: {}", event.getId());
                metricsProvider.incrementCounter("outbox.event.processed", "type", event.getEventType());
            } catch (Exception e) {
                log.error("Failed to process outbox event: {}", event.getId(), e);
                metricsProvider.incrementCounter("outbox.event.error", "type", event.getEventType());
            }
        }
    }

    @Scheduled(cron = "0 0 2 * * *")
    @jakarta.transaction.Transactional
    public void cleanupOutbox() {

        Instant cutoff =
                Instant.now().minus(30, ChronoUnit.DAYS);

        outboxRepo.deleteOldProcessedEvents(
                cutoff
        );
    }
}
