package com.example.employeeservice.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "employee_list_view")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeListView {
    @Id
    private UUID id;
    private String fullName;
    private String position;
    private String departmentName;
    private String email;
    private String status;
    private String imageUrl;
}
