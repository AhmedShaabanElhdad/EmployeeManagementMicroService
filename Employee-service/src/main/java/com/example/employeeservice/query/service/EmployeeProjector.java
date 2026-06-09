package com.example.employeeservice.query.service;

import com.example.employeeservice.entity.Employee;
import com.example.employeeservice.entity.EmployeeListView;
import com.example.employeeservice.repo.EmployeeQueryRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmployeeProjector {
    private final EmployeeQueryRepo queryRepo;

    public void project(Employee employee, String departmentName) {
        EmployeeListView view = EmployeeListView.builder()
                .id(employee.getId())
                .fullName(employee.getFirstName() + " " + employee.getLastName())
                .position(employee.getPosition())
                .departmentName(departmentName)
                .email(employee.getEmail())
                .status(employee.getStatus().name())
                .imageUrl(employee.getImageUrl())
                .build();
        queryRepo.save(view);
    }

    public void update(Employee employee) {
        queryRepo.findById(employee.getId()).ifPresent(view -> {
            view.setFullName(employee.getFirstName() + " " + employee.getLastName());
            view.setPosition(employee.getPosition());
            view.setEmail(employee.getEmail());
            view.setStatus(employee.getStatus().name());
            view.setImageUrl(employee.getImageUrl());
            queryRepo.save(view);
        });
    }

    public void delete(UUID employeeId) {
        queryRepo.deleteById(employeeId);
    }

    public void updateStatus(UUID employeeId, String status) {
        queryRepo.findById(employeeId).ifPresent(view -> {
            view.setStatus(status);
            queryRepo.save(view);
        });
    }
}
