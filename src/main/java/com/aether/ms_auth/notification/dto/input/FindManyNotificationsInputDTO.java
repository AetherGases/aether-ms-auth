package com.aether.ms_auth.notification.dto.input;

public record FindManyNotificationsInputDTO(
    Integer userId,
    int skip,
    int take
) {
}
