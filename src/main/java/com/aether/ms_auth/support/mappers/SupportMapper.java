package com.aether.ms_auth.support.mappers;

import com.aether.ms_auth.support.dto.input.RegisterSupportMessageInputDTO;
import com.aether.ms_auth.support.dto.request.RegisterSupportMessageRequestDTO;

public class SupportMapper {
  public static RegisterSupportMessageInputDTO convertRequestToRegisterSupportMessageInput(
      RegisterSupportMessageRequestDTO request
  ) {
    return new RegisterSupportMessageInputDTO(
        request.category(),
        request.screen().trim(),
        request.description().trim(),
        request.image() != null
            ? new RegisterSupportMessageInputDTO.Image(
                request.image().imageName(),
                request.image().imageUrl()
            )
            : null
    );
  }
}
