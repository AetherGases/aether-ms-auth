package com.aether.ms_auth.shared.services;

import com.aether.ms_auth.shared.enums.StorageFileTypeEnum;
import com.cloudinary.Cloudinary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service("cloudinaryUtilService")
@RequiredArgsConstructor
public class CloudinarySharedService {
  private final Cloudinary cloudinary;


  public CloudinarySignatureInfos generateUploadSignature(StorageFileTypeEnum fileType) {
    long timestamp = Instant.now().getEpochSecond();

    Map<String, Object> params = new HashMap<>();

    params.put("timestamp", timestamp);
    params.put("folder", fileType.getCloudinaryFolder());

    String signature = cloudinary.apiSignRequest(
        params,
        cloudinary.config.apiSecret,
        1
    );

    return new CloudinarySignatureInfos(
        signature,
        timestamp,
        cloudinary.config.apiKey,
        cloudinary.config.cloudName,
        fileType.getCloudinaryFolder(),
        fileType.getCloudinaryResourceType()
    );
  }
  public record CloudinarySignatureInfos(
      String signature,
      long timestamp,
      String apiKey,
      String cloudName,
      String cloudinaryFolder,
      String cloudinaryResourceType
  ){

  }
}