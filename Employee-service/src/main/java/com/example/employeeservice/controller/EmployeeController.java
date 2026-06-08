package com.example.employeeservice.controller;

import com.example.employeeservice.abstraction.EmployeeService;
import com.example.employeeservice.dtos.CreateEmployeeDTO;
import com.example.employeeservice.dtos.EmployeeResponseDTO;
import com.example.employeeservice.dtos.PaginatedResponse;
import com.example.employeeservice.dtos.UpdateEmployeeDTO;
import com.example.employeeservice.entity.EmployeeListView;
import core.GlobalResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    @GetMapping
    public ResponseEntity<GlobalResponse<PaginatedResponse<EmployeeListView>>> getEmployees(
            @RequestParam(defaultValue = "0") int page,
            @Max(100) @RequestParam(defaultValue = "10") int size
    ) {
        PaginatedResponse<EmployeeListView> employees = employeeService.findAll(page, size);
        return ResponseEntity.ok(new GlobalResponse<>(employees));
    }

    @GetMapping("/{employeeId}")
    public ResponseEntity<GlobalResponse<EmployeeListView>> getEmployee(@PathVariable UUID employeeId) {
        EmployeeListView employee = employeeService.findEmployeeById(employeeId);
        return ResponseEntity.ok(new GlobalResponse<>(employee));
    }

    @DeleteMapping("/{employeeId}")
    public ResponseEntity<Void> deleteEmployee(@PathVariable UUID employeeId) {
        employeeService.deleteEmployee(employeeId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{employeeId}")
    public ResponseEntity<GlobalResponse<EmployeeResponseDTO>> updateEmployee(
            @PathVariable UUID employeeId,
            @RequestBody @Valid UpdateEmployeeDTO updateEmployeeDTO
    ) {
        EmployeeResponseDTO updatedEmployee = employeeService.updateEmployee(employeeId, updateEmployeeDTO);
        return ResponseEntity.ok(new GlobalResponse<>(updatedEmployee));
    }

    @PostMapping
    public ResponseEntity<GlobalResponse<EmployeeResponseDTO>> create(
            @RequestBody @Valid CreateEmployeeDTO createEmployeeDTO
    ) {
        EmployeeResponseDTO insertedEmployee = employeeService.createEmployee(createEmployeeDTO);
        return new ResponseEntity<>(new GlobalResponse<>(insertedEmployee), HttpStatus.CREATED);
    }
}
