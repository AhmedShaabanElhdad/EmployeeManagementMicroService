package com.example.employeeservice.repo;

import com.example.employeeservice.entity.Outbox;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@Repository
public interface OutboxRepo extends R2dbcRepository<Outbox, UUID> {
    Flux<Outbox> findByProcessedFalse();

    @Modifying
    @Query("DELETE FROM outbox WHERE processed = true AND processed_at < :cutoff")
    Mono<Void> deleteOldProcessedEvents(Instant cutoff);
}
