package com.binitech.pdv.adapters.inbound.web;

import com.binitech.pdv.domain.ImageKey;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ImageUrlResolver {

  private final String baseUrl;

  public ImageUrlResolver(
      @Value("${images.public-base-url:}") String baseUrl,
      @Value("${images.origin-path:/api/public/images}") String originPath) {
    String base = baseUrl == null || baseUrl.isBlank() ? originPath : baseUrl.strip();
    while (base.endsWith("/")) {
      base = base.substring(0, base.length() - 1);
    }
    if (!base.startsWith("/") && !base.startsWith("https://") && !base.startsWith("http://")) {
      throw new IllegalStateException(
          "IMAGES_PUBLIC_BASE_URL precisa começar com https:// ou http://");
    }
    this.baseUrl = base;
  }

  @Named("imageUrl")
  public String urlOf(String key) {
    String path = ImageKey.relativePath(key);
    return path == null ? null : baseUrl + "/" + path;
  }
}
