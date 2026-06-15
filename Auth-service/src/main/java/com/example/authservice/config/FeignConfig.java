package com.example.authservice.config;

import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignConfig {

    // todo think of using mTLS / OAuth Client Credentials / Service Mesh after make comparison 
    @Value("${internal.api.secret}")
    private String secret;

    @Bean
    public RequestInterceptor requestInterceptor() {

        return template ->
                template.header(
                        "X-Internal-Secret",
                        secret
                );
    }

    @Bean
    public ErrorDecoder errorDecoder() {
        return new FeignErrorDecoder();
    }
}