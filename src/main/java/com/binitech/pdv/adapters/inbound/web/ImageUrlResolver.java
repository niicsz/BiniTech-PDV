package com.binitech.pdv.adapters.inbound.web;

import com.binitech.pdv.domain.ImageKey;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ImageUrlResolver {

  static final String ORIGIN_PATH = "/api/public/images";

  private final String baseUrl;

  public ImageUrlResolver(@Value("${images.public-base-url:}") String baseUrl) {
    String base = baseUrl == null || baseUrl.isBlank() ? ORIGIN_PATH : baseUrl.strip();
    while (base.endsWith("/")) {
      base = base.substring(0, base.length() - 1);
    }
    this.baseUrl = base;
  }

  @Named("imageUrl")
  public String urlOf(String key) {
    String path = ImageKey.relativePath(key);
    return path == null ? null : baseUrl + "/" + path;
  }
}
