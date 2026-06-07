package com.example.payrollservice.repo;

import com.example.payrollservice.entity.LeaveRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface LeaveRequestRepo extends JpaRepository<LeaveRequest, UUID> {
    List<LeaveRequest> findByEmployeeIdAndStatusAndStartDateBetween(
            UUID employeeId, 
            LeaveRequest.LeaveStatus status, 
            LocalDate start, 
            LocalDate end
    );
}
