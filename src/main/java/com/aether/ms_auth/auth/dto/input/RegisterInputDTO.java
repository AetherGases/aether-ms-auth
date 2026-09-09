package com.aether.ms_auth.auth.dto.input;

public record RegisterInputDTO(
    String cpf,
    String name,
    String email,
    String password,
    String phone
) {
}
