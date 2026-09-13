package com.aether.ms_auth.profile.dto.output;

public record UpdateProfileOutputDTO(
    Integer id,
    String name,
    String cpf,
    String email,
    String phone
) {
}
