package com.example.payrollservice.service;

import com.example.payrollservice.entity.LeaveRequest;
import com.example.payrollservice.entity.Payroll;
import com.example.payrollservice.entity.PayrollInvoice;

import java.util.List;
import java.util.UUID;

public interface PayrollService {
    Payroll getPayrollByEmployeeId(UUID employeeId);
    
    LeaveRequest requestLeave(UUID employeeId, LeaveRequest leaveRequest);
    
    void approveLeave(UUID leaveId);
    
    PayrollInvoice generateMonthlyInvoice(UUID employeeId, String month);
    
    List<PayrollInvoice> getInvoicesByEmployeeId(UUID employeeId);
}
