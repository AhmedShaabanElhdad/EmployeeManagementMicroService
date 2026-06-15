package com.example.employeeservice.gateway;

import com.example.employeeservice.dtos.DepartmentResponse;
import com.example.shared.grpc.DepartmentGrpcServiceGrpc;
import com.example.shared.grpc.DepartmentRequest;
import com.example.shared.core.GlobalResponse;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;

import net.devh.boot.grpc.client.inject.GrpcClient;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DepartmentGateway {

    @GrpcClient("departmentService")
    private DepartmentGrpcServiceGrpc.DepartmentGrpcServiceBlockingStub departmentStub;

    @CircuitBreaker(
            name = "departmentService",
            fallbackMethod = "departmentFallback"
    )
    public ResponseEntity<GlobalResponse<DepartmentResponse>> getDepartment(UUID departmentId) {

        DepartmentRequest request = DepartmentRequest.newBuilder()
                .setDepartmentId(departmentId.toString())
                .build();

        var grpcResponse = departmentStub.getDepartment(request);

        DepartmentResponse response = new DepartmentResponse(
                UUID.fromString(grpcResponse.getId()),
                grpcResponse.getName()
        );

        return ResponseEntity.ok(new GlobalResponse<>(response));
    }

    public ResponseEntity<GlobalResponse<DepartmentResponse>> departmentFallback(
            UUID departmentId,
            Throwable ex
    ) {
        return new ResponseEntity<>(new GlobalResponse<>(new DepartmentResponse(
                departmentId,
                "UNKNOWN_DEPARTMENT"
        )), HttpStatus.NOT_FOUND);
    }
}
