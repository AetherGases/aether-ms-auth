package com.aether.ms_auth.profile.dto.output;

import java.util.List;

public record GetMyProfileOutputDTO(
    Integer id,
    String cpf,
    String email,
    String name,
    List<GetMyProfileInfosOutputDTO.Permission> permissions
) {
}
