package com.example.payrollservice.service;

import com.example.payrollservice.entity.LeaveRequest;
import com.example.payrollservice.entity.Payroll;
import com.example.payrollservice.entity.PayrollInvoice;
import com.example.payrollservice.repo.LeaveRequestRepo;
import com.example.payrollservice.repo.PayrollInvoiceRepo;
import com.example.payrollservice.repo.PayrollRepo;
import com.example.shared.events.PayrollInvoiceEvent;
import com.example.shared.monitoring.MetricsProvider;
import com.example.shared.core.CustomResponseException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PayrollServiceImpl implements PayrollService {

    private final PayrollRepo payrollRepo;
    private final LeaveRequestRepo leaveRequestRepo;
    private final PayrollInvoiceRepo invoiceRepo;
    private final MetricsProvider metricsProvider;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public Payroll getPayrollByEmployeeId(UUID employeeId) {
        return payrollRepo.findByEmployeeId(employeeId)
                .orElseThrow(() -> CustomResponseException.ResourceNotFound("Payroll not found for employee " + employeeId));
    }

    @Override
    @Transactional
    public LeaveRequest requestLeave(UUID employeeId, LeaveRequest leaveRequest) {
        leaveRequest.setEmployeeId(employeeId);
        leaveRequest.setStatus(LeaveRequest.LeaveStatus.PENDING);
        return leaveRequestRepo.save(leaveRequest);
    }

    @Override
    @Transactional
    public void approveLeave(UUID leaveId) {
        LeaveRequest leave = leaveRequestRepo.findById(leaveId)
                .orElseThrow(() -> CustomResponseException.ResourceNotFound("Leave request not found"));
        leave.setStatus(LeaveRequest.LeaveStatus.APPROVED);
        leaveRequestRepo.save(leave);
    }

    @Override
    @Transactional
    public PayrollInvoice generateMonthlyInvoice(UUID employeeId, String month) {
        long startTime = System.currentTimeMillis();
        Payroll payroll = getPayrollByEmployeeId(employeeId);

        LocalDate now = LocalDate.now();
        // In a real app, 'month' parameter would define these dates
        LocalDate startOfMonth = now.withDayOfMonth(1);
        LocalDate endOfMonth = now.withDayOfMonth(now.lengthOfMonth());

        // Fetch approved unpaid leaves
        List<LeaveRequest> unpaidLeaves = leaveRequestRepo.findByEmployeeIdAndStatusAndStartDateBetween(
                employeeId,
                LeaveRequest.LeaveStatus.APPROVED,
                startOfMonth,
                endOfMonth
        ).stream().filter(l -> l.getType() == LeaveRequest.LeaveType.VACATION_UNPAID).toList();

        long unpaidDays = 0;
        for (LeaveRequest leave : unpaidLeaves) {
            unpaidDays += ChronoUnit.DAYS.between(leave.getStartDate(), leave.getEndDate()) + 1;
        }

        // Calculation Logic: Gross to Net
        BigDecimal dailyRate = payroll.getGrossSalary().divide(new BigDecimal("30"), 2, RoundingMode.HALF_UP);
        BigDecimal deductions = dailyRate.multiply(new BigDecimal(unpaidDays));

        BigDecimal taxableAmount = payroll.getGrossSalary().subtract(deductions);
        BigDecimal taxAmount = taxableAmount.multiply(payroll.getTaxRate());
        BigDecimal netAmount = taxableAmount.subtract(taxAmount);

        PayrollInvoice invoice = PayrollInvoice.builder()
                .employeeId(employeeId)
                .invoiceDate(LocalDate.now())
                .grossAmount(payroll.getGrossSalary())
                .taxAmount(taxAmount)
                .deductions(deductions)
                .netAmount(netAmount)
                .month(month)
                .status("SENT")
                .build();

        PayrollInvoice savedInvoice = invoiceRepo.save(invoice);

        // Automated Email via Kafka Event
        // In this complex scenario, we assume email is needed. 
        // We'll pass a placeholder or implement an Employee Service lookup if needed.
        PayrollInvoiceEvent event = new PayrollInvoiceEvent(
                employeeId,
                savedInvoice.getId(),
                "employee@company.com", // This should ideally be fetched from Employee Service
                netAmount,
                month,
                "PAYROLL_INVOICE"
        );

        kafkaTemplate.send("payroll-invoice-topic", event);

        metricsProvider.recordExecutionTime("payroll.invoice.generate.time", System.currentTimeMillis() - startTime);
        log.info("Invoice generated and sent to Kafka for employee: {}", employeeId);
        return savedInvoice;
    }

    @Override
    public List<PayrollInvoice> getInvoicesByEmployeeId(UUID employeeId) {
        return invoiceRepo.findByEmployeeId(employeeId);
    }
}
