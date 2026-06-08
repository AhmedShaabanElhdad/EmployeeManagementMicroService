package com.example.payrollservice.dtos;

import com.example.payrollservice.entity.LeaveRequest;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

public record LeaveRequestDTO(
        UUID id,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        @NotNull LeaveRequest.LeaveType type,
        LeaveRequest.LeaveStatus status
) {
}
