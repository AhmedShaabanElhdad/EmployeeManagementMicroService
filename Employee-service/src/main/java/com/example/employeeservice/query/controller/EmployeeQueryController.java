package com.example.employeeservice.query.controller;

import com.example.employeeservice.entity.EmployeeListView;
import com.example.employeeservice.repo.EmployeeQueryRepo;
import core.GlobalResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/employees/search")
@RequiredArgsConstructor
public class EmployeeQueryController {
    private final EmployeeQueryRepo queryRepo;

    @GetMapping
    public GlobalResponse<List<EmployeeListView>> search(@RequestParam String name) {
        return new GlobalResponse<>(queryRepo.findByFullNameContainingIgnoreCase(name));
    }
}
