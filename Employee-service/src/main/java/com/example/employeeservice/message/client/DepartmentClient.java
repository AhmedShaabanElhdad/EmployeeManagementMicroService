package com.example.employeeservice.message.client;

import com.example.employeeservice.dtos.DepartmentResponse;
import com.example.shared.core.GlobalResponse;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "department-service")
@Deprecated
public interface DepartmentClient {

    @GetMapping("/api/v1/departments/{departmentId}")
    ResponseEntity<GlobalResponse<DepartmentResponse>> findDepartmentById(
            @PathVariable UUID departmentId
    );
}
