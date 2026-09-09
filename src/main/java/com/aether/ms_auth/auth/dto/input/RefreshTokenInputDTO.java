package com.aether.ms_auth.auth.dto.input;

public record RefreshTokenInputDTO(
    String email,
    String refreshToken
) {
}
