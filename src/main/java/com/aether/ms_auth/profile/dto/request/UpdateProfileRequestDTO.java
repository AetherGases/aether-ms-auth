package com.aether.ms_auth.profile.dto.request;

import com.aether.ms_auth.shared.AetherConstants;
import com.aether.ms_auth.shared.helpers.RegexPatterns;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

public record UpdateProfileRequestDTO(
    @Size(
        min = AetherConstants.MIN_EMPLOYEE_NAME_LENGTH,
        max = AetherConstants.MAX_EMPLOYEE_NAME_LENGTH,
        message = "{validation.name.size}"
    )
    @Pattern(
        regexp = RegexPatterns.NAME,
        message = "{validation.name.regex}"
    )
    @Schema(
        description = "Novo nome do usuário",
        example = "José Benício da Silva Rodrigues"
    )
    String name,

    @NotEmpty(message = "{validation.phone.required}")
    @Pattern(
        regexp = RegexPatterns.PHONE,
        message = "{validation.phone.regex}"
    )
    @Schema(
        description = "Novo telefone do usuário logado",
        example = "(11) 99999-9999"
    )
    String phone,

    @Size(
        min = AetherConstants.MIN_PASSWORD_LENGTH,
        max = AetherConstants.MAX_PASSWORD_LENGTH,
        message = "{validation.password.size}"
    )
    @Pattern(
        regexp = RegexPatterns.PASSWORD,
        message = "{validation.password.regex}"
    )
    @Schema(
        description = "Nova senha do usuário autenticado",
        example = "Senha123"
    )
    String newPassword,

    ImageRequest image
) {
  public record ImageRequest(
      @NotEmpty(message = "{validation.image-name.required}")
      @Size(
          min = AetherConstants.MIN_STORAGE_FILE_NAME_LENGTH,
          max = AetherConstants.MAX_STORAGE_FILE_NAME_LENGTH,
          message = "{validation.image-name.size}"
      )
      @Schema(
          description = "Nome da nova imagem de perfil",
          example = "novaImagem.png"
      )
      String imageName,

      @NotEmpty(message = "{validation.image-url.required}")
      @Size(
          min = AetherConstants.MIN_STORAGE_FILE_PATH_LENGTH,
          max = AetherConstants.MAX_STORAGE_FILE_PATH_LENGTH,
          message = "{validation.image-url.size}"
      )
      @URL(
          protocol = "https",
          message = "{validation.image-url.regex}"
      )
      @Schema(
          description = "URL da nova imagem do avatar",
          example = "https://cloudinary/imagem.png"
      )
      String imageUrl
  ) {}
}
