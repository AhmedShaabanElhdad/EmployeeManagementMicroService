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
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    @GetMapping
    public Mono<ResponseEntity<GlobalResponse<PaginatedResponse<EmployeeListView>>>> getEmployees(
            @RequestParam(defaultValue = "0") int page,
            @Max(100) @RequestParam(defaultValue = "10") int size
    ) {
        return employeeService.findAll(page, size)
                .map(employees -> ResponseEntity.ok(new GlobalResponse<>(employees)));
    }

    @GetMapping("/{employeeId}")
    public Mono<ResponseEntity<GlobalResponse<EmployeeListView>>> getEmployee(@PathVariable UUID employeeId) {
        return employeeService.findEmployeeById(employeeId)
                .map(employee -> ResponseEntity.ok(new GlobalResponse<>(employee)));
    }

    @DeleteMapping("/{employeeId}")
    public Mono<ResponseEntity<Void>> deleteEmployee(@PathVariable UUID employeeId) {
        return employeeService.deleteEmployee(employeeId)
                .thenReturn(ResponseEntity.noContent().build());
    }

    @PutMapping("/{employeeId}")
    public Mono<ResponseEntity<GlobalResponse<EmployeeResponseDTO>>> updateEmployee(
            @PathVariable UUID employeeId,
            @RequestBody @Valid UpdateEmployeeDTO updateEmployeeDTO
    ) {
        return employeeService.updateEmployee(employeeId, updateEmployeeDTO)
                .map(updatedEmployee -> ResponseEntity.ok(new GlobalResponse<>(updatedEmployee)));
    }

    @PostMapping
    public Mono<ResponseEntity<GlobalResponse<EmployeeResponseDTO>>> create(
            @RequestBody @Valid CreateEmployeeDTO createEmployeeDTO
    ) {
        return employeeService.createEmployee(createEmployeeDTO)
                .map(insertedEmployee -> new ResponseEntity<>(new GlobalResponse<>(insertedEmployee), HttpStatus.CREATED));
    }

    @PostMapping("/{employeeId}/image")
    public Mono<ResponseEntity<GlobalResponse<EmployeeResponseDTO>>> uploadImage(
            @PathVariable UUID employeeId,
            @RequestPart("file") Mono<FilePart> file
    ) {
        return employeeService.uploadEmployeeImage(employeeId, file)
                .map(response -> ResponseEntity.ok(new GlobalResponse<>(response)));
    }
}
