package com.example.employeeservice.config;

import com.example.employeeservice.entity.IdempotencyKey;
import com.example.employeeservice.repo.IdempodentKeyRepo;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;


// todo check Race condition. for DataIntegrityViolationException
@Component
@RequiredArgsConstructor
public class IdempotencyFilter extends OncePerRequestFilter {

    private static final String IDEMPOTENCY_HEADER =
            "Idempotency-Key";

    private final IdempodentKeyRepo repository;

    @Override
    protected boolean shouldNotFilter(
            HttpServletRequest request
    ) {

        return HttpMethod.GET.matches(request.getMethod());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String key = request.getHeader(IDEMPOTENCY_HEADER);

        if (key == null || key.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        boolean alreadyProcessed = repository.existsById(key);

        if (alreadyProcessed) {

            response.setStatus(HttpStatus.CONFLICT.value());
            response.getWriter().write(
                    "Duplicate request detected"
            );

            return;
        }

        try {
            repository.save(
                    new IdempotencyKey(
                            key,
                            request.getRequestURI(),
                            request.getMethod()
                    )
            );

        } catch (DataIntegrityViolationException ex) {
            response.setStatus(HttpStatus.CONFLICT.value());
            response.getWriter().write("Duplicate request");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
