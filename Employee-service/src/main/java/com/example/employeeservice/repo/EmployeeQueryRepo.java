package com.example.employeeservice.repo;

import com.example.employeeservice.entity.EmployeeListView;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.util.UUID;

@Repository
public interface EmployeeQueryRepo extends R2dbcRepository<EmployeeListView, UUID> {
    Flux<EmployeeListView> findByFullNameContainingIgnoreCase(String name);
    
    Flux<EmployeeListView> findAllBy(Pageable pageable);
}
