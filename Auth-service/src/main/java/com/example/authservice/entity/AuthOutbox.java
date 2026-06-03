package com.example.authservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "auth_outbox")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthOutbox {

    @Id
    @GeneratedValue(generator = "UUID")
    @UuidGenerator
    private UUID id;

    @Column(nullable = false)
    private String aggregateId;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant processedAt;

    @Column(nullable = false)
    private boolean processed = false;
}
