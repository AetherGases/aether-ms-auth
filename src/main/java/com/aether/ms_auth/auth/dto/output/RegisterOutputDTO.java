package com.aether.ms_auth.auth.dto.output;

public record RegisterOutputDTO(
    Integer id,
    String cpf,
    String name,
    String email,
    String phone
) {
}
