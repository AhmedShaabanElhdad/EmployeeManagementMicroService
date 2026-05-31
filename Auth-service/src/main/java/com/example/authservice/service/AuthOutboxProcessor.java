package com.example.authservice.service;

import com.example.authservice.config.KafkaProducerConfig;
import com.example.authservice.dtos.UserIdRequestDTO;
import com.example.authservice.entity.AuthOutbox;
import com.example.authservice.repo.AuthOutboxRepo;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthOutboxProcessor {

    private final AuthOutboxRepo outboxRepo;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void processOutboxEvents() {
        List<AuthOutbox> unprocessedEvents = outboxRepo.findByProcessedFalse();

        for (AuthOutbox event : unprocessedEvents) {
            try {
                if ("EmployeeVerificationInitiated".equals(event.getEventType())) {
                    UserIdRequestDTO payload = objectMapper.readValue(event.getPayload(), UserIdRequestDTO.class);
                    kafkaTemplate.send(KafkaProducerConfig.VERIFY_TOPIC, payload);
                }
                
                event.setProcessed(true);
                event.setProcessedAt(Instant.now());
                outboxRepo.save(event);
                
                log.info("Successfully processed auth outbox event: {}", event.getId());
            } catch (Exception e) {
                log.error("Failed to process auth outbox event: {}", event.getId(), e);
            }
        }
    }
}
