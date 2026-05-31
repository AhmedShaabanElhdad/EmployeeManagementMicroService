package com.example.employeeservice.repo;

import com.example.employeeservice.entity.IdempotencyKey;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

// todo Add cleanup job for idempotency_keys.
@Repository
public interface IdempodentKeyRepo extends JpaRepository<IdempotencyKey, String> {

}
