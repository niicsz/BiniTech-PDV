package com.binitech.pdv.application.ports.outbound;

import com.binitech.pdv.domain.StoredImage;
import java.util.Optional;

public interface ImageStoragePort {

  boolean available();

  void put(String key, StoredImage image);

  Optional<StoredImage> get(String key);

  void delete(String key);
}
