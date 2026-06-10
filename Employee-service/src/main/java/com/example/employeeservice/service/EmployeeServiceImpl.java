package com.example.employeeservice.service;

import com.example.employeeservice.abstraction.EmployeeService;
import com.example.employeeservice.dtos.CreateEmployeeDTO;
import com.example.employeeservice.dtos.EmployeeCreatedEvent;
import com.example.employeeservice.dtos.EmployeeResponse;
import com.example.employeeservice.dtos.EmployeeResponseDTO;
import com.example.employeeservice.dtos.PaginatedResponse;
import com.example.employeeservice.dtos.UpdateEmployeeDTO;
import com.example.employeeservice.entity.Employee;
import com.example.employeeservice.entity.EmployeeListView;
import com.example.employeeservice.entity.Outbox;
import com.example.employeeservice.gateway.DepartmentGateway;
import com.example.employeeservice.mapper.Mapper;
import com.example.employeeservice.query.service.EmployeeProjector;
import com.example.employeeservice.repo.EmployeeQueryRepo;
import com.example.employeeservice.repo.EmployeeRepo;
import com.example.employeeservice.repo.OutboxRepo;
import com.example.shared.events.EmployeeSagaEvent;
import com.example.shared.monitoring.MetricsProvider;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

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
    private final EmployeeQueryRepo employeeQueryRepo;
    private final OutboxRepo outboxRepo;
    private final DepartmentGateway departmentGateway;
    private final ObjectMapper objectMapper;
    private final MetricsProvider metricsProvider;
    private final EmployeeProjector employeeProjector;
    private final S3FileStorageService s3FileStorageService;

    @Override
    public Mono<PaginatedResponse<EmployeeListView>> findAll(int page, int size) {
        long startTime = System.currentTimeMillis();
        return employeeQueryRepo.findAllBy(PageRequest.of(page, size))
                .collectList()
                .zipWith(employeeQueryRepo.count())
                .map(tuple -> {
                    List<EmployeeListView> content = tuple.getT1();
                    long total = tuple.getT2();
                    int totalPages = (int) Math.ceil((double) total / size);
                    
                    PaginatedResponse<EmployeeListView> response = new PaginatedResponse<>(
                            content,
                            totalPages,
                            page,
                            total,
                            page < totalPages - 1,
                            page > 0,
                            null,
                            null
                    );
                    metricsProvider.recordExecutionTime("employee.find.all.time", System.currentTimeMillis() - startTime);
                    return response;
                });
    }

    @Override
    public Mono<EmployeeListView> findEmployeeById(UUID employeeId) {
        long startTime = System.currentTimeMillis();
        log.info("Fetching employee from Read Model for ID: {}", employeeId);
        return employeeQueryRepo.findById(employeeId)
                .switchIfEmpty(Mono.error(() -> {
                    metricsProvider.incrementCounter("employee.find.error", "type", "not_found");
                    return CustomResponseException.ResourceNotFound("Employee with Id " + employeeId + " not found in Read Model");
                }))
                .doOnSuccess(v -> metricsProvider.recordExecutionTime("employee.find.by.id.time", System.currentTimeMillis() - startTime));
    }

    @Override
    @Transactional
    public Mono<EmployeeResponseDTO> updateEmployee(UUID employeeId, UpdateEmployeeDTO employee) {
        metricsProvider.incrementCounter("employee.update.request");
        return employeeRepo.findById(employeeId)
                .switchIfEmpty(Mono.error(CustomResponseException.ResourceNotFound("Employee with Id " + employeeId + " not found")))
                .flatMap(existingEmployee -> employeeRepo.existsByEmailAndIdNot(employee.email(), employeeId)
                        .flatMap(exists -> {
                            if (exists) {
                                metricsProvider.incrementCounter("employee.update.error", "reason", "email_exists");
                                return Mono.error(CustomResponseException.BadRequest("Email already exists"));
                            }
                            existingEmployee.setFirstName(employee.firstName());
                            existingEmployee.setLastName(employee.lastName());
                            existingEmployee.setPosition(employee.position());
                            existingEmployee.setPhoneNumber(employee.phoneNumber());
                            existingEmployee.setEmail(employee.email());
                            return employeeRepo.save(existingEmployee);
                        })
                )
                .flatMap(savedEmployee -> employeeProjector.update(savedEmployee).thenReturn(savedEmployee))
                .doOnSuccess(v -> metricsProvider.incrementCounter("employee.update.success"))
                .map(Mapper::toResponseDTO);
    }

    @Override
    @Transactional
    public Mono<Void> deleteEmployee(UUID employeeId) {
        return employeeRepo.existsById(employeeId)
                .flatMap(exists -> {
                    if (!exists) {
                        return Mono.error(CustomResponseException.ResourceNotFound("Employee with Id " + employeeId + " not found"));
                    }
                    return employeeRepo.deleteById(employeeId);
                })
                .then(employeeProjector.delete(employeeId))
                .doOnSuccess(v -> metricsProvider.incrementCounter("employee.delete.success"));
    }

    @Override
    @Transactional
    public Mono<EmployeeResponseDTO> createEmployee(CreateEmployeeDTO createEmployeeDTO) {
        metricsProvider.incrementCounter("employee.create.request");
        return departmentGateway.getDepartment(createEmployeeDTO.departmentId())
                .flatMap(response -> {
                    if (response == null || response.data == null || "UNKNOWN_DEPARTMENT".equals(response.data.name())) {
                        metricsProvider.incrementCounter("employee.create.error", "reason", "department_not_found");
                        return Mono.error(CustomResponseException.ResourceNotFound("Department with Id " + createEmployeeDTO.departmentId() + " not found or unavailable"));
                    }
                    return employeeRepo.existsByEmail(createEmployeeDTO.email())
                            .flatMap(exists -> {
                                if (exists) {
                                    metricsProvider.incrementCounter("employee.create.error", "reason", "email_exists");
                                    return Mono.error(CustomResponseException.BadRequest("Email already exists"));
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
                                
                                return employeeRepo.save(employee)
                                        .flatMap(savedEmployee -> {
                                            EmployeeCreatedEvent notificationEvent = new EmployeeCreatedEvent(savedEmployee.getEmail(), token);
                                            EmployeeSagaEvent sagaEvent = new EmployeeSagaEvent(
                                                    savedEmployee.getId(),
                                                    savedEmployee.getEmail(),
                                                    savedEmployee.getFirstName(),
                                                    savedEmployee.getLastName(),
                                                    "PENDING"
                                            );
                                            try {
                                                List<Outbox> events = List.of(
                                                        Outbox.builder()
                                                                .aggregateId(savedEmployee.getId().toString())
                                                                .aggregateType("Employee")
                                                                .eventType("EmployeeCreated")
                                                                .eventId(UUID.randomUUID())
                                                                .payload(objectMapper.writeValueAsString(notificationEvent))
                                                                .createdAt(Instant.now())
                                                                .processed(false)
                                                                .build(),
                                                        Outbox.builder()
                                                                .aggregateId(savedEmployee.getId().toString())
                                                                .aggregateType("Employee")
                                                                .eventType("EmployeeSagaStart")
                                                                .eventId(UUID.randomUUID())
                                                                .payload(objectMapper.writeValueAsString(sagaEvent))
                                                                .createdAt(Instant.now())
                                                                .processed(false)
                                                                .build()
                                                );
                                                return outboxRepo.saveAll(events).collectList()
                                                        .then(employeeProjector.project(savedEmployee, response.data.name()))
                                                        .thenReturn(savedEmployee);
                                            } catch (JsonProcessingException e) {
                                                return Mono.error(CustomResponseException.InternalServerError("Internal Server Error during event serialization"));
                                            }
                                        });
                            });
                })
                .doOnSuccess(v -> metricsProvider.incrementCounter("employee.create.success"))
                .map(Mapper::toResponseDTO);
    }

    @Override
    public Mono<EmployeeResponse> findByToken(String token) {
        return employeeRepo.findOneByAccountCreationToken(token)
                .map(employee -> new EmployeeResponse(
                        employee.getId(),
                        employee.isVerified(),
                        employee.getEmail()
                ))
                .switchIfEmpty(Mono.error(CustomResponseException.ResourceNotFound("Employee not found")));
    }

    @Override
    @Transactional
    public Mono<EmployeeResponse> verifyEmployee(String userId) {
        return employeeRepo.findById(UUID.fromString(userId))
                .switchIfEmpty(Mono.error(CustomResponseException.ResourceNotFound("Employee not found")))
                .flatMap(employee -> {
                    employee.setVerified(true);
                    employee.setAccountCreationToken(null);
                    return employeeRepo.save(employee);
                })
                .doOnSuccess(v -> metricsProvider.incrementCounter("employee.verify.success"))
                .map(employee -> new EmployeeResponse(
                        employee.getId(),
                        employee.isVerified(),
                        employee.getEmail()
                ));
    }

    @Override
    @Transactional
    public Mono<Void> updateEmployeeStatus(UUID employeeId, Employee.Status status) {
        return employeeRepo.findById(employeeId)
                .switchIfEmpty(Mono.error(CustomResponseException.ResourceNotFound("Employee not found")))
                .flatMap(employee -> {
                    employee.setStatus(status);
                    return employeeRepo.save(employee);
                })
                .flatMap(savedEmployee -> employeeProjector.updateStatus(employeeId, status.name()))
                .doOnSuccess(v -> {
                    log.info("Employee {} status updated to {}", employeeId, status);
                    metricsProvider.incrementCounter("employee.status.update", "status", status.name());
                })
                .then();
    }

    @Override
    @Transactional
    public Mono<EmployeeResponseDTO> uploadEmployeeImage(UUID employeeId, Mono<FilePart> fileMono) {
        return employeeRepo.findById(employeeId)
                .switchIfEmpty(Mono.error(CustomResponseException.ResourceNotFound("Employee not found")))
                .flatMap(employee -> fileMono.flatMap(s3FileStorageService::uploadFile)
                        .flatMap(imageUrl -> {
                            employee.setImageUrl(imageUrl);
                            return employeeRepo.save(employee);
                        })
                )
                .flatMap(savedEmployee -> employeeProjector.update(savedEmployee).thenReturn(savedEmployee))
                .map(Mapper::toResponseDTO);
    }
}
