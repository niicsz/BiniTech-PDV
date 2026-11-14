package com.binitech.pdv.config;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.binitech.pdv.adapters.outbound.storage.DisabledImageStorageAdapter;
import com.binitech.pdv.adapters.outbound.storage.FileSystemImageStorageAdapter;
import com.binitech.pdv.adapters.outbound.storage.S3ImageStorageAdapter;
import com.binitech.pdv.config.ImageConfig.S3Settings;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.awssdk.services.s3.S3Client;

class ImageConfigTest {

  private static final S3Settings EMPTY = new S3Settings("", "auto", "", "", "", false);
  private static final S3Settings FULL =
      new S3Settings("https://storage.example.com", "auto", "imagens", "chave", "segredo", true);

  @Test
  @DisplayName("Sem configuração, ou com S3 incompleto, o envio fica desligado")
  void disabledByDefault() {
    assertInstanceOf(DisabledImageStorageAdapter.class, ImageConfig.storageFor(null, null, EMPTY));
    assertInstanceOf(
        DisabledImageStorageAdapter.class, ImageConfig.storageFor("none", null, EMPTY));
    assertInstanceOf(DisabledImageStorageAdapter.class, ImageConfig.storageFor("s3", null, EMPTY));
  }

  @Test
  @DisplayName("Pasta local e S3 completos ligam o adaptador certo")
  void enabled(@TempDir Path dir) {
    assertInstanceOf(
        FileSystemImageStorageAdapter.class,
        ImageConfig.storageFor("filesystem", dir.toString(), EMPTY));
    assertInstanceOf(
        FileSystemImageStorageAdapter.class, ImageConfig.storageFor("FILESYSTEM", "", EMPTY));
    assertInstanceOf(S3ImageStorageAdapter.class, ImageConfig.storageFor("s3", null, FULL));
  }

  @Test
  @DisplayName("Cliente S3 aceita endpoint próprio e região vazia")
  void s3Client() {
    try (S3Client client =
        ImageConfig.s3Client(new S3Settings("", "", "imagens", "chave", "segredo", false))) {
      assertNotNull(client);
    }
  }
}
