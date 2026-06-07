package com.example.payrollservice.repo;

import com.example.payrollservice.entity.PayrollInvoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PayrollInvoiceRepo extends JpaRepository<PayrollInvoice, UUID> {
    List<PayrollInvoice> findByEmployeeId(UUID employeeId);
}
