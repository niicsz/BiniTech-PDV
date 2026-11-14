package com.binitech.pdv.adapters.outbound.storage;

import com.binitech.pdv.application.ports.outbound.ImageStoragePort;
import com.binitech.pdv.domain.ImageKey;
import com.binitech.pdv.domain.StoredImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class FileSystemImageStorageAdapter implements ImageStoragePort {

  private final Path root;

  public FileSystemImageStorageAdapter(Path root) {
    this.root = root.toAbsolutePath().normalize();
  }

  @Override
  public boolean available() {
    return true;
  }

  @Override
  public void put(String key, StoredImage image) {
    Path file = resolve(key);
    try {
      Files.createDirectories(file.getParent());
      Files.write(file, image.content());
    } catch (IOException exception) {
      throw new UncheckedIOException(exception);
    }
  }

  @Override
  public Optional<StoredImage> get(String key) {
    Path file = resolve(key);
    if (!Files.isRegularFile(file)) {
      return Optional.empty();
    }
    try {
      return Optional.of(new StoredImage(Files.readAllBytes(file), ImageKey.contentTypeOf(key)));
    } catch (IOException exception) {
      throw new UncheckedIOException(exception);
    }
  }

  @Override
  public void delete(String key) {
    try {
      Files.deleteIfExists(resolve(key));
    } catch (IOException exception) {
      throw new UncheckedIOException(exception);
    }
  }

  private Path resolve(String key) {
    Path file = root.resolve(key).normalize();
    if (!file.startsWith(root)) {
      throw new IllegalArgumentException("Chave de imagem inválida.");
    }
    return file;
  }
}
