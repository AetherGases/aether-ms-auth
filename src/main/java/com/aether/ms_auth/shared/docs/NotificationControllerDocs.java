package com.aether.ms_auth.shared.docs;

import com.aether.ms_auth.notification.dto.output.FindManyNotificationsOutputDTO;
import com.aether.ms_auth.notification.dto.query.FindManyNotificationsQueryParamsDTO;
import org.springdoc.core.annotations.ParameterObject;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

public interface NotificationControllerDocs {
  @Operation(
      summary = "Lista as notificações do usuário logado.",
      description = "Retorna as notificações do usuário logado, da mais recente para a mais antiga, com paginação por skip e take.",
      tags = {"Notification"},
      responses = {
          @ApiResponse(description = "Success", responseCode = "200", content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = FindManyNotificationsOutputDTO.class)
            )
          ),
          @ApiResponse(description = "Bad Request", responseCode = "400", content = @Content),
          @ApiResponse(description = "Unhautorized", responseCode = "401", content = @Content),
          @ApiResponse(description = "Forbidden", responseCode = "403", content = @Content),
          @ApiResponse(description = "Internal Server Error", responseCode = "500", content = @Content)
      }
  )
  ResponseEntity<FindManyNotificationsOutputDTO> findManyNotifications(
      Authentication authentication,
      @ParameterObject
      FindManyNotificationsQueryParamsDTO queryParams
  );
}
