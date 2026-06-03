package com.example.departmentservice.service;

import com.example.departmentservice.entity.Department;
import com.example.departmentservice.repo.DepartmentRepo;
import com.example.shared.grpc.DepartmentGrpcServiceGrpc;
import com.example.shared.grpc.DepartmentRequest;
import com.example.shared.grpc.DepartmentResponse;

import net.devh.boot.grpc.server.service.GrpcService;

import java.util.UUID;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.springframework.cache.annotation.Cacheable;
import lombok.RequiredArgsConstructor;

@GrpcService
@RequiredArgsConstructor
public class DepartmentGrpcServiceImpl extends DepartmentGrpcServiceGrpc.DepartmentGrpcServiceImplBase {

    private final DepartmentRepo departmentRepo;

    @Cacheable(
            value = "departments",
            key = "#request.departmentId"
    )
    @Override
    public void getDepartment(DepartmentRequest request, StreamObserver<DepartmentResponse> responseObserver) {
        try {
            UUID departmentId = UUID.fromString(request.getDepartmentId());
            Department department = departmentRepo.findById(departmentId)
                    .orElseThrow(() ->
                            Status.NOT_FOUND
                                    .withDescription("Department not found")
                                    .asRuntimeException()
                    );

            DepartmentResponse response = DepartmentResponse.newBuilder()
                    .setId(department.getId().toString())
                    .setName(department.getName())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (IllegalArgumentException e) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("Invalid UUID format")
                    .asRuntimeException()
            );
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription("An internal error occurred")
                    .asRuntimeException()
            );
        }
    }
}
