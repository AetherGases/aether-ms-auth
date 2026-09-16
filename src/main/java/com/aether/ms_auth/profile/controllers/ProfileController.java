package com.aether.ms_auth.profile.controllers;

import com.aether.ms_auth.profile.dto.input.GetMyProfileInputDTO;
import com.aether.ms_auth.profile.dto.output.GetMyProfileOutputDTO;
import com.aether.ms_auth.profile.dto.output.UpdateProfileOutputDTO;
import com.aether.ms_auth.profile.dto.request.UpdateProfileRequestDTO;
import com.aether.ms_auth.profile.mappers.ProfileMapper;
import com.aether.ms_auth.profile.services.ProfileService;
import com.aether.ms_auth.shared.docs.ProfileControllerDocs;
import com.aether.ms_auth.shared.helpers.CustomUserDetails;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Profile", description = "Rotas para visualização e atualização do usuário logado.")
@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController implements ProfileControllerDocs {
  private final ProfileService profileService;

  @Override
  @GetMapping()
  public ResponseEntity<GetMyProfileOutputDTO> getMyProfile(
      Authentication authentication
  ){
    CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();

    return new ResponseEntity<>(
        this.profileService.getMyProfile(
            new GetMyProfileInputDTO(user.getId())
        ),
        HttpStatus.OK
    );
  }

  @Override
  @PatchMapping()
  public ResponseEntity<UpdateProfileOutputDTO> updateProfile(
      Authentication authentication,

      @RequestBody
      @Valid
      UpdateProfileRequestDTO input
  ){
    CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();

    return new ResponseEntity<>(
        this.profileService.updateUser(
            ProfileMapper.convertRequestToUpdateProfileInput(input, user.getId())
        ),
        HttpStatus.OK
    );
  }
}
