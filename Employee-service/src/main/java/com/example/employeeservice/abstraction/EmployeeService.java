package com.example.employeeservice.abstraction;

import com.example.employeeservice.dtos.CreateEmployeeDTO;
import com.example.employeeservice.dtos.EmployeeResponse;
import com.example.employeeservice.dtos.EmployeeResponseDTO;
import com.example.employeeservice.dtos.PaginatedResponse;
import com.example.employeeservice.dtos.UpdateEmployeeDTO;
import com.example.employeeservice.entity.Employee;
import com.example.employeeservice.entity.EmployeeListView;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface EmployeeService {
    // Read from Projection
    Mono<PaginatedResponse<EmployeeListView>> findAll(int page, int size);

    // Read from Projection
    Mono<EmployeeListView> findEmployeeById(UUID id);

    // Command Operations
    Mono<EmployeeResponseDTO> updateEmployee(UUID id, UpdateEmployeeDTO updateEmployeeDTO);

    Mono<Void> deleteEmployee(UUID id);

    Mono<EmployeeResponseDTO> createEmployee(CreateEmployeeDTO createEmployeeDTO);

    Mono<EmployeeResponse> findByToken(String token);

    Mono<EmployeeResponse> verifyEmployee(String userId);

    Mono<Void> updateEmployeeStatus(UUID employeeId, Employee.Status status);

    Mono<EmployeeResponseDTO> uploadEmployeeImage(UUID employeeId, Mono<FilePart> file);
}
