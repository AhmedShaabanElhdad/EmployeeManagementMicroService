package com.example.employeeservice.repo;

import com.example.employeeservice.entity.Outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxRepo extends JpaRepository<Outbox, UUID> {
    List<Outbox> findByProcessedFalse();

    @Modifying
    @Query("""
                DELETE FROM Outbox o
                WHERE o.processed = true
                AND o.processedAt < :cutoff
            """)
    void deleteOldProcessedEvents(
            Instant cutoff
    );
}
