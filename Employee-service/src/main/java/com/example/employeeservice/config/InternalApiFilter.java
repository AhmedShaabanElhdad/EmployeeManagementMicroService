package com.example.employeeservice.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class InternalApiFilter extends OncePerRequestFilter {

    @Value("${internal.api.secret}")
    private String secret;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (!path.startsWith("/internal")) {
            filterChain.doFilter(request, response);
            return;
        }

        String internalHeader = request.getHeader("X-Internal-Secret");

        if (secret == null || secret.isEmpty() || !secret.equals(internalHeader)) {
            log.warn("Unauthorized internal access attempt to path: {} from IP: {}. Secret match: {}", 
                path, request.getRemoteAddr(), secret != null && secret.equals(internalHeader));
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid Internal Secret");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
