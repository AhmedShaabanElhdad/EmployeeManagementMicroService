package com.example.payrollservice.service;

import com.example.payrollservice.entity.Payroll;
import com.example.payrollservice.repo.PayrollRepo;
import com.example.shared.events.EmployeeSagaEvent;
import com.example.shared.events.PayrollSagaResponseEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class PayrollSagaConsumer {

    private final PayrollRepo payrollRepo;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @KafkaListener(topics = "employee-saga-topic", groupId = "payroll-group")
    @Transactional
    public void consumeEmployeeCreated(EmployeeSagaEvent event) {
        log.info("Received EmployeeSagaEvent for employee: {}", event.employeeId());
        
        try {
            // Business Logic: Create payroll for new employee
            // Default gross salary and tax rate for new employees
            Payroll payroll = Payroll.builder()
                    .employeeId(event.employeeId())
                    .grossSalary(new BigDecimal("5000.00"))
                    .taxRate(new BigDecimal("0.20")) // Default 20% tax
                    .status("ACTIVE")
                    .build();
            
            payrollRepo.save(payroll);
            
            log.info("Payroll created for employee: {}", event.employeeId());
            
            // Send Success Response
            publishResponse(event.employeeId(), true, "Payroll created successfully");
            
        } catch (Exception e) {
            log.error("Failed to create payroll for employee: {}", event.employeeId(), e);
            // Send Failure Response
            publishResponse(event.employeeId(), false, e.getMessage());
        }
    }

    private void publishResponse(java.util.UUID employeeId, boolean success, String reason) {
        PayrollSagaResponseEvent responseEvent = new PayrollSagaResponseEvent(employeeId, success, reason);
        kafkaTemplate.send("payroll-saga-response-topic", responseEvent);
    }
}
