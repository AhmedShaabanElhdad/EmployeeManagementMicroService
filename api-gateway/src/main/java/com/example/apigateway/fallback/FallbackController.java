package com.example.apigateway.fallback;

import core.GlobalResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
public class FallbackController {

    @GetMapping("/fallback/employees")
    public Mono<GlobalResponse<Void>> employeeFallback() {
        return Mono.just(new GlobalResponse<>(List.of(
                new GlobalResponse.ErrorItem("Employee Service is currently unavailable. Please try again later.")
        )));
    }

    @GetMapping("/fallback/auth")
    public Mono<GlobalResponse<Void>> authFallback() {
        return Mono.just(new GlobalResponse<>(List.of(
                new GlobalResponse.ErrorItem("Authentication Service is currently unavailable.")
        )));
    }

    @GetMapping("/fallback/department")
    public Mono<GlobalResponse<Void>> departmentFallback() {
        return Mono.just(new GlobalResponse<>(List.of(
                new GlobalResponse.ErrorItem("Department Service is currently unavailable.")
        )));
    }
}
