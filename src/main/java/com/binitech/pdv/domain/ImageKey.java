package com.binitech.pdv.domain;

import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

public final class ImageKey {

  private static final String PREFIX = "images/";
  private static final String PUBLIC_PATH = "/api/public/images/";
  private static final Pattern TENANT = Pattern.compile("[A-Za-z0-9_-]{1,64}");
  private static final Pattern FILE = Pattern.compile("[0-9a-f]{32}\.(jpg|png)");

  private ImageKey() {}

  public static String newKey(String tenantId, String extension) {
    if (tenantId == null || !TENANT.matcher(tenantId).matches()) {
      throw new IllegalArgumentException("Loja inválida para guardar imagem.");
    }
    UUID id = UUID.randomUUID();
    HexFormat hex = HexFormat.of();
    String name =
        hex.toHexDigits(id.getMostSignificantBits())
            + hex.toHexDigits(id.getLeastSignificantBits());
    return PREFIX + tenantId + "/" + name + "." + extension;
  }

  public static Optional<String> of(String tenantId, String file) {
    if (tenantId == null
        || file == null
        || !TENANT.matcher(tenantId).matches()
        || !FILE.matcher(file).matches()) {
      return Optional.empty();
    }
    return Optional.of(PREFIX + tenantId + "/" + file);
  }

  public static String publicPath(String key) {
    if (key == null || !key.startsWith(PREFIX)) {
      return null;
    }
    return PUBLIC_PATH + key.substring(PREFIX.length());
  }

  public static String contentTypeOf(String key) {
    return key != null && key.endsWith(".png") ? "image/png" : "image/jpeg";
  }
}
