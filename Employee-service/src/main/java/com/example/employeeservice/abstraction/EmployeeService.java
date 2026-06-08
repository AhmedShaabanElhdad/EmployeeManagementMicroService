package com.example.employeeservice.abstraction;

import com.example.employeeservice.dtos.CreateEmployeeDTO;
import com.example.employeeservice.dtos.EmployeeResponse;
import com.example.employeeservice.dtos.EmployeeResponseDTO;
import com.example.employeeservice.dtos.PaginatedResponse;
import com.example.employeeservice.dtos.UpdateEmployeeDTO;
import com.example.employeeservice.entity.Employee;
import com.example.employeeservice.entity.EmployeeListView;

import java.util.UUID;

public interface EmployeeService {
    // Read from Projection
    PaginatedResponse<EmployeeListView> findAll(int page, int size);

    // Read from Projection
    EmployeeListView findEmployeeById(UUID id);

    // Command Operations
    EmployeeResponseDTO updateEmployee(UUID id, UpdateEmployeeDTO updateEmployeeDTO);

    void deleteEmployee(UUID id);

    EmployeeResponseDTO createEmployee(CreateEmployeeDTO createEmployeeDTO);

    EmployeeResponse findByToken(String token);

    EmployeeResponse verifyEmployee(String userId);

    void updateEmployeeStatus(UUID employeeId, Employee.Status status);
}
