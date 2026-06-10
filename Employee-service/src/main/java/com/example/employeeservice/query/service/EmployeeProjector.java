package com.example.employeeservice.query.service;

import com.example.employeeservice.entity.Employee;
import com.example.employeeservice.entity.EmployeeListView;
import com.example.employeeservice.repo.EmployeeQueryRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmployeeProjector {
    private final EmployeeQueryRepo queryRepo;

    public Mono<Void> project(Employee employee, String departmentName) {
        EmployeeListView view = EmployeeListView.builder()
                .id(employee.getId())
                .fullName(employee.getFirstName() + " " + employee.getLastName())
                .position(employee.getPosition())
                .departmentName(departmentName)
                .email(employee.getEmail())
                .status(employee.getStatus().name())
                .imageUrl(employee.getImageUrl())
                .build();
        return queryRepo.save(view).then();
    }

    public Mono<Void> update(Employee employee) {
        return queryRepo.findById(employee.getId())
                .flatMap(view -> {
                    view.setFullName(employee.getFirstName() + " " + employee.getLastName());
                    view.setPosition(employee.getPosition());
                    view.setEmail(employee.getEmail());
                    view.setStatus(employee.getStatus().name());
                    view.setImageUrl(employee.getImageUrl());
                    return queryRepo.save(view);
                }).then();
    }

    public Mono<Void> delete(UUID employeeId) {
        return queryRepo.deleteById(employeeId);
    }

    public Mono<Void> updateStatus(UUID employeeId, String status) {
        return queryRepo.findById(employeeId)
                .flatMap(view -> {
                    view.setStatus(status);
                    return queryRepo.save(view);
                }).then();
    }
}
