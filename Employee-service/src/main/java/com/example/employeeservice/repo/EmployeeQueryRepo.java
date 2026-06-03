package com.example.employeeservice.repo;

import com.example.employeeservice.entity.EmployeeListView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EmployeeQueryRepo extends JpaRepository<EmployeeListView, UUID> {
    List<EmployeeListView> findByFullNameContainingIgnoreCase(String name);
}
