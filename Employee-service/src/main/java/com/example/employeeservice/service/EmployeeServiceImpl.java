package com.example.employeeservice.service;

import com.example.employeeservice.abstraction.EmployeeService;
import com.example.employeeservice.dtos.CreateEmployeeDTO;
import com.example.employeeservice.dtos.EmployeeCreatedEvent;
import com.example.employeeservice.dtos.EmployeeResponse;
import com.example.employeeservice.dtos.EmployeeResponseDTO;
import com.example.employeeservice.dtos.UpdateEmployeeDTO;
import com.example.employeeservice.entity.Employee;
import com.example.employeeservice.entity.Outbox;
import com.example.employeeservice.gateway.DepartmentGateway;
import com.example.employeeservice.mapper.Mapper;
import com.example.employeeservice.repo.EmployeeRepo;
import com.example.employeeservice.repo.OutboxRepo;
import com.example.shared.events.EmployeeSagaEvent;
import com.example.shared.monitoring.MetricsProvider;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import core.CustomResponseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepo employeeRepo;
    private final OutboxRepo outboxRepo;
    private final DepartmentGateway departmentGateway;
    private final ObjectMapper objectMapper;
    private final MetricsProvider metricsProvider;

    @Override
    public Page<Employee> findAll(int page, int size) {
        long startTime = System.currentTimeMillis();
        Pageable pageable = PageRequest.of(page, size);
        Page<Employee> result = employeeRepo.findAll(pageable);
        metricsProvider.recordExecutionTime("employee.find.all.time", System.currentTimeMillis() - startTime);
        return result;
    }

    @Override
    @Cacheable(value = "employees", key = "#employeeId")
    public EmployeeResponseDTO findEmployeeById(UUID employeeId) {
        long startTime = System.currentTimeMillis();
        log.info("Fetching employee from DB for ID: {}", employeeId);
        Employee employeeEntity = employeeRepo.findById(employeeId)
                .orElseThrow(() -> {
                    metricsProvider.incrementCounter("employee.find.error", "type", "not_found");
                    return CustomResponseException.ResourceNotFound("Employee with Id " + employeeId + " not found");
                });

        metricsProvider.recordExecutionTime("employee.find.by.id.time", System.currentTimeMillis() - startTime);
        return Mapper.toResponseDTO(employeeEntity);
    }

    @Override
    @Transactional
    @CachePut(value = "employees", key = "#employeeId")
    public EmployeeResponseDTO updateEmployee(UUID employeeId, UpdateEmployeeDTO employee) {
        metricsProvider.incrementCounter("employee.update.request");
        Employee updatedEmployee = employeeRepo.findById(employeeId)
                .orElseThrow(() -> CustomResponseException.ResourceNotFound(
                        "Employee with Id " + employeeId + " not found"
                ));

        if (employeeRepo.existsByEmailAndIdNot(employee.email(), employeeId)) {
            metricsProvider.incrementCounter("employee.update.error", "reason", "email_exists");
            throw CustomResponseException.BadRequest("Email already exists");
        }

        updatedEmployee.setFirstName(employee.firstName());
        updatedEmployee.setLastName(employee.lastName());
        updatedEmployee.setPosition(employee.position());
        updatedEmployee.setPhoneNumber(employee.phoneNumber());
        updatedEmployee.setEmail(employee.email());

        Employee employeeEntity = employeeRepo.save(updatedEmployee);
        metricsProvider.incrementCounter("employee.update.success");
        return Mapper.toResponseDTO(employeeEntity);
    }

    @Transactional
    @Override
    @CacheEvict(value = "employees", key = "#employeeId")
    public void deleteEmployee(UUID employeeId) {
        if (!employeeRepo.existsById(employeeId)) {
            throw CustomResponseException.ResourceNotFound("Employee with Id " + employeeId + " not found");
        }
        employeeRepo.deleteById(employeeId);
        metricsProvider.incrementCounter("employee.delete.success");
    }

    @Transactional
    @Override
    public EmployeeResponseDTO createEmployee(CreateEmployeeDTO createEmployeeDTO) {
        // todo move validation for department outside transaction
        metricsProvider.incrementCounter("employee.create.request");
        var response = departmentGateway.getDepartment(createEmployeeDTO.departmentId()).getBody();

        if (response == null || response.data == null || "UNKNOWN_DEPARTMENT".equals(response.data.name())) {
            metricsProvider.incrementCounter("employee.create.error", "reason", "department_not_found");
            throw CustomResponseException.ResourceNotFound("Department with Id " + createEmployeeDTO.departmentId() + " not found or unavailable");
        }

        if (employeeRepo.existsByEmail(createEmployeeDTO.email())) {
            metricsProvider.incrementCounter("employee.create.error", "reason", "email_exists");
            throw CustomResponseException.BadRequest("Email already exists");
        }

        Employee employee = new Employee();
        String token = UUID.randomUUID().toString();
        employee.setAccountCreationToken(token);
        employee.setVerified(false);
        employee.setEmail(createEmployeeDTO.email());
        employee.setPosition(createEmployeeDTO.position());
        employee.setFirstName(createEmployeeDTO.firstName());
        employee.setLastName(createEmployeeDTO.lastName());
        employee.setHireAt(createEmployeeDTO.hireAt());
        employee.setPhoneNumber(createEmployeeDTO.phoneNumber());
        employee.setDepartmentId(response.data.id());
        employee.setStatus(Employee.Status.PENDING);

        Employee savedEmployee = employeeRepo.save(employee);

        EmployeeCreatedEvent notificationEvent = new EmployeeCreatedEvent(savedEmployee.getEmail(), token);
        EmployeeSagaEvent sagaEvent = new EmployeeSagaEvent(
                savedEmployee.getId(),
                savedEmployee.getEmail(),
                savedEmployee.getFirstName(),
                savedEmployee.getLastName(),
                "PENDING"
        );

        try {
            outboxRepo.saveAll(
                    // todo add event type to enum + event creation into a helper
                    List.of(
                            Outbox.builder()
                                    .aggregateId(savedEmployee.getId().toString())
                                    .aggregateType("Employee")
                                    .eventType("EmployeeCreated")
                                    .eventId(UUID.randomUUID())
                                    .payload(objectMapper.writeValueAsString(notificationEvent))
                                    .createdAt(Instant.now())
                                    .processed(false)
                                    .build()
                            ,
                            Outbox.builder()
                                    .aggregateId(savedEmployee.getId().toString())
                                    .aggregateType("Employee")
                                    .eventType("EmployeeSagaStart")
                                    .eventId(UUID.randomUUID())
                                    .payload(objectMapper.writeValueAsString(sagaEvent))
                                    .createdAt(Instant.now())
                                    .processed(false)
                                    .build()
                    )
            );


        } catch (JsonProcessingException e) {
            log.error("Failed to serialize events", e);
            throw CustomResponseException.BadRequest("Internal Server Error during event serialization");
        }

        metricsProvider.incrementCounter("employee.create.success");
        return Mapper.toResponseDTO(savedEmployee);
    }

    // todo two caching key fix
    @Override
    @Cacheable(value = "employees", key = "#token")
    public EmployeeResponse findByToken(String token) {
        return employeeRepo.findOneByAccountCreationToken(token)
                .map(employee -> new EmployeeResponse(
                        employee.getId(),
                        employee.isVerified(),
                        employee.getEmail()
                ))
                .orElseThrow(() -> CustomResponseException.ResourceNotFound("Employee not found"));
    }

    @Override
    @Transactional
    @CacheEvict(value = "employees", allEntries = true)
    public EmployeeResponse verifyEmployee(String userId) {
        Employee employee = employeeRepo.findById(UUID.fromString(userId))
                .orElseThrow(() -> CustomResponseException.ResourceNotFound("Employee not found"));

        employee.setVerified(true);
        employee.setAccountCreationToken(null);
        employeeRepo.save(employee);
        metricsProvider.incrementCounter("employee.verify.success");

        return new EmployeeResponse(
                employee.getId(),
                employee.isVerified(),
                employee.getEmail()
        );
    }

    @Override
    @Transactional
    public void updateEmployeeStatus(UUID employeeId, Employee.Status status) {
        Employee employee = employeeRepo.findById(employeeId)
                .orElseThrow(() -> CustomResponseException.ResourceNotFound("Employee not found"));
        employee.setStatus(status);
        employeeRepo.save(employee);
        log.info("Employee {} status updated to {}", employeeId, status);
        metricsProvider.incrementCounter("employee.status.update", "status", status.name());
    }
}
