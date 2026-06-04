package com.example.payrollservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableDiscoveryClient
@ComponentScan(basePackages = {"com.example.payrollservice", "com.example.shared"})
public class PayrollServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(PayrollServiceApplication.class, args);
    }
}
