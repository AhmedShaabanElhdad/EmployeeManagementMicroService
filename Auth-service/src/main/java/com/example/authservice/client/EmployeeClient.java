package com.example.authservice.client;

import com.example.authservice.dtos.EmployeeResponse;
import com.example.authservice.dtos.UserIdRequestDTO;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

// todo add fallbackFactory or fallback
@FeignClient(name = "employee-service")
public interface EmployeeClient {
    @GetMapping("/internal/employees/by-token")
    EmployeeResponse getEmployeeByToken(@RequestParam String token);

    @PostMapping("/internal/employees/verify")
    void verify(@RequestBody UserIdRequestDTO userIdRequestDTO);
}


