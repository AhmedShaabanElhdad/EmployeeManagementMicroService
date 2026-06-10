package com.example.employeeservice.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Table("outbox")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Outbox {

    @Id
    private UUID id;

    @Column("event_id")
    private UUID eventId;

    @Column("aggregate_id")
    private String aggregateId;

    @Column("aggregate_type")
    private String aggregateType;

    @Column("event_type")
    private String eventType;

    @Column("payload")
    private String payload;

    @Column("created_at")
    private Instant createdAt;

    @Column("processed_at")
    private Instant processedAt;

    @Column("processed")
    @Builder.Default
    private boolean processed = false;
}
