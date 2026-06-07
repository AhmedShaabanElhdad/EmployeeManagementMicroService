package com.example.shared.events;

import java.math.BigDecimal;
import java.util.UUID;

public record PayrollInvoiceEvent(
        UUID employeeId,
        UUID invoiceId,
        String email,
        BigDecimal netAmount,
        String month,
        String type
) {
}
