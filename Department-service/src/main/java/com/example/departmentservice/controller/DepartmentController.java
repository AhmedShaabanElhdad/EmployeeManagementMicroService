package com.example.departmentservice.controller;

import com.example.departmentservice.abstraction.DepartmentService;
import com.example.departmentservice.dtos.CreateDepartmentRequest;
import com.example.departmentservice.entity.Department;
import com.example.shared.core.GlobalResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    @GetMapping
    public ResponseEntity<GlobalResponse<List<Department>>> getDepartments() {
        List<Department> departments = departmentService.findAll();
        return ResponseEntity.ok(new GlobalResponse<>(departments));
    }

    @GetMapping("/{departmentId}")
    public ResponseEntity<GlobalResponse<Department>> getDepartment(@PathVariable UUID departmentId) {
        Department department = departmentService.findDepartmentById(departmentId);
        return ResponseEntity.ok(new GlobalResponse<>(department));
    }

    @DeleteMapping("/{departmentId}")
    public ResponseEntity<Void> deleteDepartment(@PathVariable UUID departmentId) {
        departmentService.deleteDepartment(departmentId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{departmentId}")
    public ResponseEntity<GlobalResponse<Department>> updateDepartment(
            @PathVariable UUID departmentId,
            @RequestParam String name
    ) {
        Department updatedDepartment = departmentService.updateDepartment(departmentId, name);
        return ResponseEntity.ok(new GlobalResponse<>(updatedDepartment));
    }

    @PostMapping
    public ResponseEntity<GlobalResponse<Department>> create(@RequestBody @Valid CreateDepartmentRequest request) {
        Department insertedDepartment = departmentService.createDepartment(request);
        return new ResponseEntity<>(new GlobalResponse<>(insertedDepartment), HttpStatus.CREATED);
    }
}
