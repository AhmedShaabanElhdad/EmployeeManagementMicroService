package com.example.employeeservice.controller;

import com.example.employeeservice.abstraction.EmployeeService;
import com.example.employeeservice.dtos.EmployeeResponse;
import com.example.employeeservice.dtos.UserIdRequestDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/internal/employees")
@RequiredArgsConstructor
public class InternalEmployeeController {
    
    private final EmployeeService employeeService;

    @GetMapping("/by-token")
    public Mono<EmployeeResponse> findByToken(@RequestParam String token) {
        return employeeService.findByToken(token);
    }

    @PostMapping("/verify")
    public Mono<EmployeeResponse> verify(@RequestBody UserIdRequestDTO userIdRequestDTO) {
        return employeeService.verifyEmployee(userIdRequestDTO.userId());
    }
}
