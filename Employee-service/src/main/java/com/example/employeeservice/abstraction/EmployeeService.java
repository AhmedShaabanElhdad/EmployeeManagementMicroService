package com.example.employeeservice.abstraction;

import com.example.employeeservice.dtos.CreateEmployeeDTO;
import com.example.employeeservice.dtos.EmployeeResponse;
import com.example.employeeservice.dtos.EmployeeResponseDTO;
import com.example.employeeservice.dtos.UpdateEmployeeDTO;
import com.example.employeeservice.entity.Employee;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface EmployeeService {
    Page<Employee> findAll(int page, int size);

    EmployeeResponseDTO findEmployeeById(UUID id);

    EmployeeResponseDTO updateEmployee(UUID id, UpdateEmployeeDTO updateEmployeeDTO);

    void deleteEmployee(UUID id);

    EmployeeResponseDTO createEmployee(CreateEmployeeDTO createEmployeeDTO);

    EmployeeResponse findByToken(String token);

    EmployeeResponse verifyEmployee(String userId);
}
