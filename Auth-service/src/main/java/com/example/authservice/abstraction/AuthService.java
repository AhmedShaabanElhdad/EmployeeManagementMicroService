package com.example.authservice.abstraction;

import com.example.authservice.dtos.*;

public interface AuthService {

    UserResponseDTO signup(SignUpRequestDTO signUpRequestDTO, String token);

    AuthResponseDTO login(LoginRequestDTO loginRequestDTO);

    AuthResponseDTO refresh(RefreshTokenRequestDTO refreshTokenRequestDTO);

    void logout(String token);
}
