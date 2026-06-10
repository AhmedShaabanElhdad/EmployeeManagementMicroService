package com.example.employeeservice.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Table("employee_list_view")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeListView {
    @Id
    private UUID id;
    
    @Column("full_name")
    private String fullName;
    
    @Column("position")
    private String position;
    
    @Column("department_name")
    private String departmentName;
    
    @Column("email")
    private String email;
    
    @Column("status")
    private String status;
    
    @Column("image_url")
    private String imageUrl;
}
