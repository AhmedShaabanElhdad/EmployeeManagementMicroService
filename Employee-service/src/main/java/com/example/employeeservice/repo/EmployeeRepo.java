package com.example.employeeservice.repo;

import com.example.employeeservice.entity.Employee;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface EmployeeRepo extends R2dbcRepository<Employee, UUID> {
    Mono<Employee> findOneByAccountCreationToken(String token);

    Mono<Boolean> existsByEmail(String email);

    Mono<Boolean> existsByEmailAndIdNot(String email, UUID id);
    
    Mono<Long> countByStatus(Employee.Status status);
}
