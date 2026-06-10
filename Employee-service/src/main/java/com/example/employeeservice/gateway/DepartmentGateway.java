package com.example.employeeservice.gateway;

import com.example.employeeservice.dtos.DepartmentResponse;
import com.example.shared.grpc.DepartmentGrpcServiceGrpc;
import com.example.shared.grpc.DepartmentRequest;
import com.example.shared.grpc.DepartmentResponseGrpc;
import core.GlobalResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DepartmentGateway {

    @GrpcClient("departmentService")
    private DepartmentGrpcServiceGrpc.DepartmentGrpcServiceStub departmentStub;

    @CircuitBreaker(
            name = "departmentService",
            fallbackMethod = "departmentFallback"
    )
    public Mono<GlobalResponse<DepartmentResponse>> getDepartment(UUID departmentId) {
        return Mono.create(sink -> {
            DepartmentRequest request = DepartmentRequest.newBuilder()
                    .setDepartmentId(departmentId.toString())
                    .build();

            departmentStub.getDepartment(request, new StreamObserver<DepartmentResponseGrpc>() {
                @Override
                public void onNext(DepartmentResponseGrpc grpcResponse) {
                    DepartmentResponse response = new DepartmentResponse(
                            UUID.fromString(grpcResponse.getId()),
                            grpcResponse.getName()
                    );
                    sink.success(new GlobalResponse<>(response));
                }

                @Override
                public void onError(Throwable t) {
                    sink.error(t);
                }

                @Override
                public void onCompleted() {
                    // No-op
                }
            });
        });
    }

    public Mono<GlobalResponse<DepartmentResponse>> departmentFallback(
            UUID departmentId,
            Throwable ex
    ) {
        return Mono.just(new GlobalResponse<>(new DepartmentResponse(
                departmentId,
                "UNKNOWN_DEPARTMENT"
        )));
    }
}
