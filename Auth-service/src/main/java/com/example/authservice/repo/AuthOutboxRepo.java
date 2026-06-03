package com.example.authservice.repo;

import com.example.authservice.entity.AuthOutbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AuthOutboxRepo extends JpaRepository<AuthOutbox, UUID> {
    List<AuthOutbox> findByProcessedFalse();
}
