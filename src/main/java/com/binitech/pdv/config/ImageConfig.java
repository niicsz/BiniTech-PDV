package com.binitech.pdv.config;

import com.binitech.pdv.adapters.outbound.image.ImageIoProcessorAdapter;
import com.binitech.pdv.adapters.outbound.storage.DisabledImageStorageAdapter;
import com.binitech.pdv.adapters.outbound.storage.FileSystemImageStorageAdapter;
import com.binitech.pdv.adapters.outbound.storage.S3ImageStorageAdapter;
import com.binitech.pdv.application.ports.inbound.ImageUseCasePort;
import com.binitech.pdv.application.ports.outbound.DeliveryStoreRepositoryPort;
import com.binitech.pdv.application.ports.outbound.ImageProcessorPort;
import com.binitech.pdv.application.ports.outbound.ImageStoragePort;
import com.binitech.pdv.application.ports.outbound.ProductRepositoryPort;
import com.binitech.pdv.application.ports.outbound.TenantRepositoryPort;
import com.binitech.pdv.application.usecases.ImageUseCaseImpl;
import java.net.URI;
import java.nio.file.Path;
import java.time.Clock;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

@Configuration
public class ImageConfig {

  private static final Logger log = LoggerFactory.getLogger(ImageConfig.class);

  public record S3Settings(
      String endpoint,
      String region,
      String bucket,
      String accessKeyId,
      String secretAccessKey,
      boolean pathStyle) {}

  @Bean
  public ImageStoragePort imageStoragePort(
      @Value("${images.storage:none}") String storage,
      @Value("${images.filesystem.directory:}") String directory,
      @Value("${images.s3.endpoint:}") String endpoint,
      @Value("${images.s3.region:auto}") String region,
      @Value("${images.s3.bucket:}") String bucket,
      @Value("${images.s3.access-key-id:}") String accessKeyId,
      @Value("${images.s3.secret-access-key:}") String secretAccessKey,
      @Value("${images.s3.path-style:false}") boolean pathStyle) {
    return storageFor(
        storage,
        directory,
        new S3Settings(endpoint, region, bucket, accessKeyId, secretAccessKey, pathStyle));
  }

  static ImageStoragePort storageFor(String storage, String directory, S3Settings s3) {
    String mode = storage == null ? "none" : storage.strip().toLowerCase(Locale.ROOT);
    switch (mode) {
      case "s3" -> {
        if (isBlank(s3.bucket()) || isBlank(s3.accessKeyId()) || isBlank(s3.secretAccessKey())) {
          log.warn("Imagens: IMAGES_STORAGE=s3 sem bucket ou credenciais; envio desligado");
          return new DisabledImageStorageAdapter();
        }
        log.info("Imagens: bucket S3 ativo");
        return new S3ImageStorageAdapter(s3Client(s3), s3.bucket().strip());
      }
      case "filesystem" -> {
        Path root =
            isBlank(directory)
                ? Path.of(System.getProperty("java.io.tmpdir"), "binitech-images")
                : Path.of(directory.strip());
        log.info("Imagens: pasta local {}", root);
        return new FileSystemImageStorageAdapter(root);
      }
      default -> {
        log.info("Imagens: envio desligado");
        return new DisabledImageStorageAdapter();
      }
    }
  }

  static S3Client s3Client(S3Settings s3) {
    S3ClientBuilder builder =
        S3Client.builder()
            .httpClient(UrlConnectionHttpClient.create())
            .region(Region.of(isBlank(s3.region()) ? "auto" : s3.region().strip()))
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(
                        s3.accessKeyId().strip(), s3.secretAccessKey().strip())))
            .forcePathStyle(s3.pathStyle())
            .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
            .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED);
    if (!isBlank(s3.endpoint())) {
      builder.endpointOverride(URI.create(s3.endpoint().strip()));
    }
    return builder.build();
  }

  @Bean
  public ImageProcessorPort imageProcessorPort() {
    return new ImageIoProcessorAdapter();
  }

  @Bean
  public ImageUseCasePort imageUseCasePort(
      @Value("${delivery.enabled:false}") boolean deliveryEnabled,
      @Value("${images.max-size:8MB}") DataSize maxSize,
      ImageStoragePort imageStoragePort,
      ImageProcessorPort imageProcessorPort,
      DeliveryStoreRepositoryPort deliveryStoreRepositoryPort,
      TenantRepositoryPort tenantRepositoryPort,
      ProductRepositoryPort productRepositoryPort) {
    return new ImageUseCaseImpl(
        new ImageUseCaseImpl.Options(deliveryEnabled, maxSize.toBytes(), Clock.systemUTC()),
        imageStoragePort,
        imageProcessorPort,
        deliveryStoreRepositoryPort,
        tenantRepositoryPort,
        productRepositoryPort);
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
