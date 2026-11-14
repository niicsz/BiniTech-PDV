package com.binitech.pdv.adapters.outbound.storage;

import com.binitech.pdv.application.ports.outbound.ImageStoragePort;
import com.binitech.pdv.domain.ImageKey;
import com.binitech.pdv.domain.StoredImage;
import java.util.Optional;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

public class S3ImageStorageAdapter implements ImageStoragePort {

  static final String CACHE_CONTROL = "public, max-age=31536000, immutable";

  private final S3Client client;
  private final String bucket;

  public S3ImageStorageAdapter(S3Client client, String bucket) {
    this.client = client;
    this.bucket = bucket;
  }

  @Override
  public boolean available() {
    return true;
  }

  @Override
  public void put(String key, StoredImage image) {
    client.putObject(
        PutObjectRequest.builder()
            .bucket(bucket)
            .key(key)
            .contentType(image.contentType())
            .cacheControl(CACHE_CONTROL)
            .build(),
        RequestBody.fromBytes(image.content()));
  }

  @Override
  public Optional<StoredImage> get(String key) {
    try {
      ResponseBytes<GetObjectResponse> bytes =
          client.getObjectAsBytes(GetObjectRequest.builder().bucket(bucket).key(key).build());
      String type = bytes.response().contentType();
      return Optional.of(
          new StoredImage(
              bytes.asByteArray(),
              type == null || !type.startsWith("image/") ? ImageKey.contentTypeOf(key) : type));
    } catch (NoSuchKeyException exception) {
      return Optional.empty();
    }
  }

  @Override
  public void delete(String key) {
    client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
  }
}
