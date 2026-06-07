package com.example.payrollservice.controller;

import com.example.payrollservice.dtos.LeaveRequestDTO;
import com.example.payrollservice.entity.LeaveRequest;
import com.example.payrollservice.entity.Payroll;
import com.example.payrollservice.entity.PayrollInvoice;
import com.example.payrollservice.service.PayrollService;
import core.GlobalResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payroll")
@RequiredArgsConstructor
public class PayrollController {

    private final PayrollService payrollService;

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<GlobalResponse<Payroll>> getPayroll(@PathVariable UUID employeeId) {
        return ResponseEntity.ok(new GlobalResponse<>(payrollService.getPayrollByEmployeeId(employeeId)));
    }

    @PostMapping("/employee/{employeeId}/leave")
    public ResponseEntity<GlobalResponse<LeaveRequest>> requestLeave(
            @PathVariable UUID employeeId,
            @RequestBody @Valid LeaveRequestDTO dto) {
        
        LeaveRequest leaveRequest = LeaveRequest.builder()
                .startDate(dto.startDate())
                .endDate(dto.endDate())
                .type(dto.type())
                .build();
                
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new GlobalResponse<>(payrollService.requestLeave(employeeId, leaveRequest)));
    }

    @PutMapping("/leave/{leaveId}/approve")
    public ResponseEntity<Void> approveLeave(@PathVariable UUID leaveId) {
        payrollService.approveLeave(leaveId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/employee/{employeeId}/generate-invoice")
    public ResponseEntity<GlobalResponse<PayrollInvoice>> generateInvoice(
            @PathVariable UUID employeeId,
            @RequestParam String month) {
        return ResponseEntity.ok(new GlobalResponse<>(payrollService.generateMonthlyInvoice(employeeId, month)));
    }

    @GetMapping("/employee/{employeeId}/invoices")
    public ResponseEntity<GlobalResponse<List<PayrollInvoice>>> getInvoices(@PathVariable UUID employeeId) {
        return ResponseEntity.ok(new GlobalResponse<>(payrollService.getInvoicesByEmployeeId(employeeId)));
    }
}
