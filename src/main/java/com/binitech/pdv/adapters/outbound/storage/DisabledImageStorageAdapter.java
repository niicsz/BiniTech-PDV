package com.binitech.pdv.adapters.outbound.storage;

import com.binitech.pdv.application.ports.outbound.ImageStoragePort;
import com.binitech.pdv.domain.StoredImage;
import java.util.Objects;
import java.util.Optional;

public class DisabledImageStorageAdapter implements ImageStoragePort {

  @Override
  public boolean available() {
    return false;
  }

  @Override
  public void put(String key, StoredImage image) {
    throw new IllegalStateException("Armazenamento de imagens desligado.");
  }

  @Override
  public Optional<StoredImage> get(String key) {
    return Optional.empty();
  }

  @Override
  public void delete(String key) {
    Objects.requireNonNull(key);
  }
}
