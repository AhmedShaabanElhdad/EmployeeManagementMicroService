package com.example.employeeservice.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Table("employee")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Employee {
    @Id
    private UUID id;

    @Column("first_name")
    private String firstName;

    @Column("last_name")
    private String lastName;

    @Column("email")
    private String email;

    @Column("hire_at")
    private LocalDate hireAt;

    @Column("phone_number")
    private String phoneNumber;

    @Column("is_verified")
    private boolean isVerified;

    @Column("account_creation_token")
    private String accountCreationToken;

    @Column("position")
    private String position;

    @Column("department_id")
    private UUID departmentId;

    @Column("image_url")
    private String imageUrl;

    @Column("status")
    private Status status = Status.PENDING;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    public enum Status {
        PENDING, ACTIVE, REJECTED
    }
}
