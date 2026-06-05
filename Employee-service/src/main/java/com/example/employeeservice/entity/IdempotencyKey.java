package com.example.employeeservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "idempotency_keys")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class IdempotencyKey {

    @Id
    @Column(nullable = false, updatable = false)
    private String key;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "request_path")
    private String requestPath;

    @Column(name = "http_method", length = 10)
    private String httpMethod;

    @Column(name = "response_hash")
    private String responseHash;

    public IdempotencyKey(
            String key,
            String requestPath,
            String httpMethod
    ) {
        this.key = key;
        this.requestPath = requestPath;
        this.httpMethod = httpMethod;
        this.createdAt = Instant.now();
    }
}