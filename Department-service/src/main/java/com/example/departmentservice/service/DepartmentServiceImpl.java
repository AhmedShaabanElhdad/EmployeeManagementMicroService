package com.example.departmentservice.service;

import com.example.departmentservice.abstraction.DepartmentService;
import com.example.departmentservice.dtos.CreateDepartmentRequest;
import com.example.departmentservice.entity.Department;
import com.example.departmentservice.repo.DepartmentRepo;
import com.example.shared.monitoring.MetricsProvider;
import core.CustomResponseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepo departmentRepo;
    private final MetricsProvider metricsProvider;

    @Override
    @Cacheable(value = "departments_list")
    public List<Department> findAll() {
        long startTime = System.currentTimeMillis();
        log.info("Fetching all departments from DB");
        List<Department> result = departmentRepo.findAll();
        metricsProvider.recordExecutionTime("department.find.all.time", System.currentTimeMillis() - startTime);
        return result;
    }

    @Override
    @Cacheable(value = "departments", key = "#departmentId")
    public Department findDepartmentById(UUID departmentId) {
        long startTime = System.currentTimeMillis();
        log.info("Fetching department from DB for ID: {}", departmentId);
        Department result = departmentRepo.findById(departmentId)
                .orElseThrow(() -> {
                    metricsProvider.incrementCounter("department.find.error", "reason", "not_found");
                    return CustomResponseException.ResourceNotFound("Department with Id " + departmentId + " not found");
                });
        metricsProvider.recordExecutionTime("department.find.by.id.time", System.currentTimeMillis() - startTime);
        return result;
    }

    @Override
    @Transactional
    @CachePut(value = "departments", key = "#departmentId")
    @CacheEvict(value = "departments_list", allEntries = true)
    public Department updateDepartment(UUID departmentId, String name) {
        metricsProvider.incrementCounter("department.update.request");
        Department department = departmentRepo.findById(departmentId)
                .orElseThrow(() -> {
                    metricsProvider.incrementCounter("department.update.error", "reason", "not_found");
                    return CustomResponseException.ResourceNotFound("Department with Id " + departmentId + " not found");
                });
        department.setName(name);
        Department saved = departmentRepo.save(department);
        metricsProvider.incrementCounter("department.update.success");
        return saved;
    }

    @Override
    @Transactional
    @CacheEvict(value = {"departments", "departments_list"}, key = "#departmentId", allEntries = true)
    public void deleteDepartment(UUID departmentId) {
        if (!departmentRepo.existsById(departmentId)) {
            metricsProvider.incrementCounter("department.delete.error", "reason", "not_found");
            throw CustomResponseException.ResourceNotFound("Department with Id " + departmentId + " not found");
        }
        departmentRepo.deleteById(departmentId);
        metricsProvider.incrementCounter("department.delete.success");
    }

    @Override
    @Transactional
    @CacheEvict(value = "departments_list", allEntries = true)
    public Department createDepartment(CreateDepartmentRequest request) {
        metricsProvider.incrementCounter("department.create.request");
        Department department = new Department();
        department.setName(request.name());
        Department saved = departmentRepo.save(department);
        metricsProvider.incrementCounter("department.create.success");
        return saved;
    }
}
