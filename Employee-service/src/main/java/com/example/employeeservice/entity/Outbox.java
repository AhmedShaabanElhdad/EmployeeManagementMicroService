package com.example.employeeservice.entity;

import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// todo @Lock(PESSIMISTIC_WRITE)
@Entity
@Table(
        name = "outbox",
        indexes = {
                @Index(
                        name = "idx_outbox_processed",
                        columnList = "processed"
                )
        })
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Outbox {

    @Id
    @GeneratedValue(generator = "UUID")
    @UuidGenerator
    private UUID id;

    @Column(nullable = false)
    private UUID eventId;

    @Column(nullable = false)
    private String aggregateId;

    @Column(nullable = false)
    private String aggregateType;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant processedAt;

    @Column(nullable = false)
    @Builder.Default
    private boolean processed = false;
}
