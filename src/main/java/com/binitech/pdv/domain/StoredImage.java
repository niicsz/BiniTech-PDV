package com.binitech.pdv.domain;

import java.util.Arrays;
import java.util.Objects;

public record StoredImage(byte[] content, String contentType) {

  public StoredImage {
    content = Objects.requireNonNull(content).clone();
    Objects.requireNonNull(contentType);
  }

  @Override
  public byte[] content() {
    return content.clone();
  }

  public int size() {
    return content.length;
  }

  public String extension() {
    return "image/png".equals(contentType) ? "png" : "jpg";
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof StoredImage other
        && Arrays.equals(content, other.content)
        && contentType.equals(other.contentType);
  }

  @Override
  public int hashCode() {
    return 31 * Arrays.hashCode(content) + contentType.hashCode();
  }

  @Override
  public String toString() {
    return "StoredImage{" + contentType + ", " + content.length + " bytes}";
  }
}
