package com.binitech.pdv.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ImageKeyTest {

  @Test
  @DisplayName("Chave nova fica na pasta da loja com nome aleatório")
  void newKey() {
    String key = ImageKey.newKey("t1", "jpg");
    assertTrue(key.matches("images/t1/[0-9a-f]{32}\\.jpg"));
    assertNotEquals(key, ImageKey.newKey("t1", "jpg"));
    assertThrows(IllegalArgumentException.class, () -> ImageKey.newKey("../t1", "jpg"));
    assertThrows(IllegalArgumentException.class, () -> ImageKey.newKey(null, "jpg"));
  }

  @Test
  @DisplayName("Caminho público só aceita loja e arquivo no formato gerado")
  void parse() {
    String file = "0123456789abcdef0123456789abcdef.png";
    assertEquals(Optional.of("images/t1/" + file), ImageKey.of("t1", file));
    assertTrue(ImageKey.of("t1", "../x.png").isEmpty());
    assertTrue(ImageKey.of("t1", file.replace(".png", ".gif")).isEmpty());
    assertTrue(ImageKey.of("t/1", file).isEmpty());
    assertTrue(ImageKey.of(null, file).isEmpty());
    assertTrue(ImageKey.of("t1", null).isEmpty());
  }

  @Test
  @DisplayName("Caminho relativo e tipo saem da chave")
  void relativeAndType() {
    assertEquals("t1/a.png", ImageKey.relativePath("images/t1/a.png"));
    assertNull(ImageKey.relativePath(null));
    assertNull(ImageKey.relativePath("outra/t1/a.png"));
    assertEquals("image/png", ImageKey.contentTypeOf("images/t1/a.png"));
    assertEquals("image/jpeg", ImageKey.contentTypeOf("images/t1/a.jpg"));
  }

  @Test
  @DisplayName("StoredImage copia o conteúdo e compara pelo valor")
  void storedImage() {
    byte[] bytes = {1, 2};
    StoredImage image = new StoredImage(bytes, "image/jpeg");
    bytes[0] = 9;
    assertEquals(1, image.content()[0]);
    assertEquals(new StoredImage(new byte[] {1, 2}, "image/jpeg"), image);
    assertEquals(new StoredImage(new byte[] {1, 2}, "image/jpeg").hashCode(), image.hashCode());
    assertEquals("jpg", image.extension());
    assertEquals("png", new StoredImage(bytes, "image/png").extension());
    assertEquals(2, image.size());
    assertTrue(image.toString().contains("2 bytes"));
  }
}
