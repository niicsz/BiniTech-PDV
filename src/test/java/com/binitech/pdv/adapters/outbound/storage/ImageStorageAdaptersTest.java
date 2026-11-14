package com.binitech.pdv.adapters.outbound.storage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.binitech.pdv.domain.StoredImage;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

class ImageStorageAdaptersTest {

  private static final String KEY = "images/t1/" + "a".repeat(32) + ".png";
  private static final StoredImage IMAGE = new StoredImage(new byte[] {1, 2, 3}, "image/png");

  @Test
  @DisplayName("Pasta local grava, lê e apaga a imagem")
  void fileSystemRoundTrip(@TempDir Path root) {
    FileSystemImageStorageAdapter storage = new FileSystemImageStorageAdapter(root);

    storage.put(KEY, IMAGE);

    assertTrue(storage.available());
    assertEquals(IMAGE, storage.get(KEY).orElseThrow());
    storage.delete(KEY);
    assertTrue(storage.get(KEY).isEmpty());
  }

  @Test
  @DisplayName("Pasta local recusa chave que sai da pasta raiz")
  void fileSystemBlocksTraversal(@TempDir Path root) {
    FileSystemImageStorageAdapter storage = new FileSystemImageStorageAdapter(root);
    assertThrows(IllegalArgumentException.class, () -> storage.get("../fora.png"));
  }

  @Test
  @DisplayName("Armazenamento desligado não grava nem devolve nada")
  void disabled() {
    DisabledImageStorageAdapter storage = new DisabledImageStorageAdapter();
    assertFalse(storage.available());
    assertTrue(storage.get(KEY).isEmpty());
    storage.delete(KEY);
    assertThrows(IllegalStateException.class, () -> storage.put(KEY, IMAGE));
  }

  @Test
  @DisplayName("S3 grava com tipo e cache imutável, lê e apaga pela chave")
  void s3() {
    S3Client client = mock(S3Client.class);
    S3ImageStorageAdapter storage = new S3ImageStorageAdapter(client, "imagens");

    storage.put(KEY, IMAGE);
    ArgumentCaptor<PutObjectRequest> put = ArgumentCaptor.forClass(PutObjectRequest.class);
    verify(client).putObject(put.capture(), any(RequestBody.class));
    assertEquals("imagens", put.getValue().bucket());
    assertEquals(KEY, put.getValue().key());
    assertEquals("image/png", put.getValue().contentType());
    assertEquals(S3ImageStorageAdapter.CACHE_CONTROL, put.getValue().cacheControl());

    when(client.getObjectAsBytes(any(GetObjectRequest.class)))
        .thenReturn(
            ResponseBytes.fromByteArray(
                GetObjectResponse.builder().contentType("binary/octet-stream").build(),
                new byte[] {7}));
    StoredImage read = storage.get(KEY).orElseThrow();
    assertArrayEquals(new byte[] {7}, read.content());
    assertEquals("image/png", read.contentType());

    storage.delete(KEY);
    verify(client).deleteObject(any(DeleteObjectRequest.class));
    assertTrue(storage.available());
  }

  @Test
  @DisplayName("S3 sem o objeto devolve vazio")
  void s3Missing() {
    S3Client client = mock(S3Client.class);
    when(client.getObjectAsBytes(any(GetObjectRequest.class)))
        .thenThrow(NoSuchKeyException.builder().message("sem").build());
    assertTrue(new S3ImageStorageAdapter(client, "imagens").get(KEY).isEmpty());
  }
}
