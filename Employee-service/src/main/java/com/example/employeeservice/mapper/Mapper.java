package com.example.employeeservice.mapper;

import com.example.employeeservice.dtos.EmployeeResponseDTO;
import com.example.employeeservice.entity.Employee;

public class Mapper {
    public static EmployeeResponseDTO toResponseDTO(Employee employee) {
        return new EmployeeResponseDTO(
                employee.getId(),
                employee.getFirstName(),
                employee.getLastName(),
                employee.getEmail(),
                employee.getHireAt(),
                employee.getPhoneNumber(),
                employee.isVerified(),
                employee.getPosition(),
                employee.getDepartmentId(),
                employee.getStatus(),
                employee.getImageUrl()
        );
    }
}
