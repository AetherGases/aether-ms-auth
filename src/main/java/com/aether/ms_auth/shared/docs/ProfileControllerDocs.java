package com.aether.ms_auth.shared.docs;

import com.aether.ms_auth.profile.dto.output.GetMyProfileOutputDTO;
import com.aether.ms_auth.profile.dto.output.UpdateProfileOutputDTO;
import com.aether.ms_auth.profile.dto.request.UpdateProfileRequestDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestBody;

public interface ProfileControllerDocs {
  @Operation(
      summary = "Retorna dados do usuário logado.",
      description = "Retorna dados e permissões do usuário logado no sistema.",
      tags = {"Profile"},
      responses = {
          @ApiResponse(description = "Success", responseCode = "200", content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                array = @ArraySchema(schema = @Schema(implementation = GetMyProfileOutputDTO.class))
            )
          ),
          @ApiResponse(description = "Bad Request", responseCode = "400", content = @Content),
          @ApiResponse(description = "Unhautorized", responseCode = "401", content = @Content),
          @ApiResponse(description = "Forbidden", responseCode = "403", content = @Content),
          @ApiResponse(description = "Internal Server Error", responseCode = "500", content = @Content)
      }
  )
  ResponseEntity<GetMyProfileOutputDTO> getMyProfile(
      Authentication authentication
  );

  @Operation(
      summary = "Atualiza alguns dados do usuário logado",
      description = "Atualiza alguns dados do usuário logado.",
      tags = {"Profile"},
      responses = {
          @ApiResponse(description = "Success", responseCode = "200", content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              array = @ArraySchema(schema = @Schema(implementation = UpdateProfileOutputDTO.class))
          )
          ),
          @ApiResponse(description = "Bad Request", responseCode = "400", content = @Content),
          @ApiResponse(description = "Unhautorized", responseCode = "401", content = @Content),
          @ApiResponse(description = "Forbidden", responseCode = "403", content = @Content),
          @ApiResponse(description = "Internal Server Error", responseCode = "500", content = @Content)
      }
  )
  ResponseEntity<UpdateProfileOutputDTO> updateProfile(
      Authentication authentication,
      @Valid
      UpdateProfileRequestDTO input
  );
}
