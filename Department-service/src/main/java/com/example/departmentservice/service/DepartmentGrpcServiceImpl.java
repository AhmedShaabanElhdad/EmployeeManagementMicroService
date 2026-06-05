package com.example.departmentservice.service;

import com.example.departmentservice.abstraction.DepartmentService;
import com.example.departmentservice.entity.Department;
import com.example.shared.grpc.DepartmentGrpcServiceGrpc;
import com.example.shared.grpc.DepartmentRequest;
import com.example.shared.grpc.DepartmentResponse;

import net.devh.boot.grpc.server.service.GrpcService;

import java.util.UUID;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;

@GrpcService
@RequiredArgsConstructor
public class DepartmentGrpcServiceImpl extends DepartmentGrpcServiceGrpc.DepartmentGrpcServiceImplBase {

    private final DepartmentService departmentService;

    //    @Cacheable(
//            value = "departments",
//            key = "#request.departmentId"
//    )
    @Override
    public void getDepartment(DepartmentRequest request, StreamObserver<DepartmentResponse> responseObserver) {
        try {
            UUID departmentId = UUID.fromString(request.getDepartmentId());
            Department department = departmentService.findDepartmentById(departmentId);

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
        } catch (StatusRuntimeException ex) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription("An internal error occurred")
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
