package com.example.authservice.controller;

import com.example.authservice.abstraction.AuthService;
import com.example.authservice.dtos.AuthResponseDTO;
import com.example.authservice.dtos.LoginRequestDTO;
import com.example.authservice.dtos.RefreshTokenRequestDTO;
import com.example.authservice.dtos.SignUpRequestDTO;
import com.example.authservice.dtos.UserResponseDTO;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.shared.core.GlobalResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<GlobalResponse<UserResponseDTO>> signUp(
            @RequestBody @Valid SignUpRequestDTO signUpRequestDTO,
            @RequestParam String token
    ) {
        UserResponseDTO userResponseDTO = authService.signup(signUpRequestDTO, token);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new GlobalResponse<>(userResponseDTO));
    }

    @PostMapping("/login")
    public ResponseEntity<GlobalResponse<AuthResponseDTO>> login(
            @RequestBody @Valid LoginRequestDTO loginRequestDTO
    ) {
        AuthResponseDTO responseDTO = authService.login(loginRequestDTO);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new GlobalResponse<>(responseDTO));

    }

    @PostMapping("/refresh")
    public ResponseEntity<GlobalResponse<AuthResponseDTO>> refresh(
            @RequestBody @Valid RefreshTokenRequestDTO refreshTokenRequestDTO
    ) {
        AuthResponseDTO responseDTO = authService.refresh(refreshTokenRequestDTO);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new GlobalResponse<>(responseDTO));
    }

    @PostMapping("/logout")
    public ResponseEntity<GlobalResponse<Void>> logout(
            @RequestHeader("Authorization") String authHeader
    ) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            authService.logout(authHeader.substring(7));
        }
        return ResponseEntity.ok(new GlobalResponse<>((Void) null));
    }

}
