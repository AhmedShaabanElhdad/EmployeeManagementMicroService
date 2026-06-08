package com.example.authservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {
    @Id
    @UuidGenerator
    private UUID id;

    @Column(nullable = false)
    private String action; // LOGIN, LOGOUT, SIGNUP, FAILED_LOGIN, LOCKOUT

    private String username;

    private String details;

    @Column(nullable = false)
    private Instant timestamp;

    private String ipAddress;
}
