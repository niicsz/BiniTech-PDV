package com.binitech.pdv.domain;

import java.util.Arrays;
import java.util.Objects;

public final class StoredImage {

  private final byte[] content;
  private final String contentType;

  public StoredImage(byte[] content, String contentType) {
    this.content = Objects.requireNonNull(content).clone();
    this.contentType = Objects.requireNonNull(contentType);
  }

  public byte[] content() {
    return content.clone();
  }

  public String contentType() {
    return contentType;
  }

  public int size() {
    return content.length;
  }

  public String extension() {
    return "image/png".equals(contentType) ? "png" : "jpg";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof StoredImage other)) return false;
    return Arrays.equals(content, other.content) && contentType.equals(other.contentType);
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
