package com.binitech.pdv.adapters.outbound.image;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.binitech.pdv.domain.StoredImage;
import com.binitech.pdv.domain.exception.BusinessException;
import com.binitech.pdv.utils.enums.ImageKind;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ImageIoProcessorAdapterTest {

  private final ImageIoProcessorAdapter processor = new ImageIoProcessorAdapter();

  private static byte[] encode(BufferedImage image, String format) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ImageIO.write(image, format, out);
    return out.toByteArray();
  }

  private static BufferedImage image(int w, int h, int type) {
    BufferedImage image = new BufferedImage(w, h, type);
    Graphics2D g = image.createGraphics();
    g.setColor(Color.RED);
    g.fillRect(0, 0, w / 2, h);
    g.setColor(Color.BLUE);
    g.fillRect(w / 2, 0, w - w / 2, h);
    g.dispose();
    return image;
  }

  private static BufferedImage decode(StoredImage stored) throws IOException {
    return ImageIO.read(new ByteArrayInputStream(stored.content()));
  }

  @Test
  @DisplayName("Foto de produto vira JPEG quadrado de 600 px cortado no centro")
  void productIsSquareJpeg() throws IOException {
    StoredImage out =
        processor.process(
            encode(image(2000, 1000, BufferedImage.TYPE_INT_RGB), "jpg"), ImageKind.PRODUCT);

    BufferedImage result = decode(out);
    assertEquals("image/jpeg", out.contentType());
    assertEquals(600, result.getWidth());
    assertEquals(600, result.getHeight());
  }

  @Test
  @DisplayName("Capa vira JPEG de 1200 x 400")
  void coverIsBanner() throws IOException {
    StoredImage out =
        processor.process(
            encode(image(900, 900, BufferedImage.TYPE_INT_RGB), "png"), ImageKind.COVER);

    BufferedImage result = decode(out);
    assertEquals(1200, result.getWidth());
    assertEquals(400, result.getHeight());
  }

  @Test
  @DisplayName("Logo continua PNG com transparência e cabe em 400 px sem ampliar")
  void logoKeepsAlphaAndAspect() throws IOException {
    StoredImage big =
        processor.process(
            encode(image(1600, 800, BufferedImage.TYPE_INT_ARGB), "png"), ImageKind.LOGO);
    BufferedImage result = decode(big);
    assertEquals("image/png", big.contentType());
    assertEquals(400, result.getWidth());
    assertEquals(200, result.getHeight());
    assertTrue(result.getColorModel().hasAlpha());

    BufferedImage small =
        decode(
            processor.process(
                encode(image(120, 100, BufferedImage.TYPE_INT_ARGB), "png"), ImageKind.LOGO));
    assertEquals(120, small.getWidth());
  }

  @Test
  @DisplayName("Arquivo que não é JPG, PNG ou WebP é recusado")
  void rejectsOtherFormats() throws IOException {
    byte[] gif = encode(image(100, 100, BufferedImage.TYPE_INT_RGB), "gif");
    assertThrows(BusinessException.class, () -> processor.process(gif, ImageKind.PRODUCT));
    byte[] text = "texto qualquer".getBytes();
    assertThrows(BusinessException.class, () -> processor.process(text, ImageKind.PRODUCT));
    assertThrows(BusinessException.class, () -> processor.process(null, ImageKind.PRODUCT));
  }

  @Test
  @DisplayName("Arquivo com cabeçalho de JPEG mas corrompido é recusado")
  void rejectsCorrupted() throws IOException {
    byte[] jpeg = encode(image(200, 200, BufferedImage.TYPE_INT_RGB), "jpg");
    byte[] broken = Arrays.copyOf(jpeg, 40);
    assertThrows(BusinessException.class, () -> processor.process(broken, ImageKind.PRODUCT));
  }

  @Test
  @DisplayName("Imagem pequena demais é recusada")
  void rejectsTiny() throws IOException {
    byte[] tiny = encode(image(20, 20, BufferedImage.TYPE_INT_RGB), "png");
    BusinessException error =
        assertThrows(BusinessException.class, () -> processor.process(tiny, ImageKind.PRODUCT));
    assertTrue(error.getMessage().contains("64 px"));
  }

  @Test
  @DisplayName("Detecta WebP pela assinatura RIFF/WEBP")
  void detectsWebp() {
    byte[] header = "RIFF0000WEBPVP8 ".getBytes();
    assertEquals(
        ImageIoProcessorAdapter.Format.WEBP, ImageIoProcessorAdapter.Format.detect(header));
  }

  @Test
  @DisplayName("Orientação EXIF 6 gira a foto do celular para ficar em pé")
  void exifRotation() {
    byte[] jpeg = exifJpeg(6, true);
    assertEquals(6, ImageIoProcessorAdapter.Exif.orientation(jpeg));
    assertEquals(3, ImageIoProcessorAdapter.Exif.orientation(exifJpeg(3, false)));

    BufferedImage landscape = image(300, 100, BufferedImage.TYPE_INT_RGB);
    BufferedImage rotated = ImageIoProcessorAdapter.orient(landscape, 6);
    assertEquals(100, rotated.getWidth());
    assertEquals(300, rotated.getHeight());
    for (int o = 2; o <= 8; o++) {
      BufferedImage out = ImageIoProcessorAdapter.orient(landscape, o);
      assertEquals(o >= 5 ? 100 : 300, out.getWidth());
    }
    assertSame(landscape, ImageIoProcessorAdapter.orient(landscape, 1));
  }

  @Test
  @DisplayName("JPEG sem EXIF ou com EXIF quebrado fica na orientação 1")
  void exifMissing() throws IOException {
    byte[] plain = encode(image(100, 100, BufferedImage.TYPE_INT_RGB), "jpg");
    assertEquals(1, ImageIoProcessorAdapter.Exif.orientation(plain));
    byte[] broken = exifJpeg(6, true);
    broken[12] = 'X';
    assertEquals(1, ImageIoProcessorAdapter.Exif.orientation(broken));
  }

  private static byte[] exifJpeg(int orientation, boolean little) {
    byte[] tiff = new byte[26];
    if (little) {
      tiff[0] = 'I';
      tiff[1] = 'I';
      tiff[2] = 42;
      tiff[4] = 8;
      tiff[8] = 1;
      tiff[10] = 0x12;
      tiff[11] = 0x01;
      tiff[12] = 3;
      tiff[14] = 1;
      tiff[18] = (byte) orientation;
    } else {
      tiff[0] = 'M';
      tiff[1] = 'M';
      tiff[3] = 42;
      tiff[7] = 8;
      tiff[9] = 1;
      tiff[10] = 0x01;
      tiff[11] = 0x12;
      tiff[13] = 3;
      tiff[17] = 1;
      tiff[19] = (byte) orientation;
    }
    int length = 2 + 6 + tiff.length;
    byte[] out = new byte[4 + 2 + length + 2];
    out[0] = (byte) 0xFF;
    out[1] = (byte) 0xD8;
    out[2] = (byte) 0xFF;
    out[3] = (byte) 0xE1;
    out[4] = (byte) (length >> 8);
    out[5] = (byte) length;
    byte[] exif = {'E', 'x', 'i', 'f', 0, 0};
    System.arraycopy(exif, 0, out, 6, 6);
    System.arraycopy(tiff, 0, out, 12, tiff.length);
    out[out.length - 2] = (byte) 0xFF;
    out[out.length - 1] = (byte) 0xD9;
    return out;
  }

  @Test
  @DisplayName("Saída não carrega EXIF: GPS e modelo do celular não vão para a URL pública")
  void outputHasNoExif() throws IOException {
    byte[] photo = encode(image(800, 800, BufferedImage.TYPE_INT_RGB), "jpg");
    byte[] withExif = new byte[photo.length + 38];
    byte[] app1 = exifJpeg(6, true);
    System.arraycopy(app1, 0, withExif, 0, 38);
    System.arraycopy(photo, 2, withExif, 38, photo.length - 2);

    byte[] out = processor.process(withExif, ImageKind.PRODUCT).content();

    String raw = new String(out, java.nio.charset.StandardCharsets.ISO_8859_1);
    assertTrue(!raw.contains("Exif"));
    assertEquals(1, ImageIoProcessorAdapter.Exif.orientation(out));
  }

  @Test
  @DisplayName("Imagem grande é lida em resolução reduzida para não estourar a memória")
  void subsampling() {
    assertEquals(1, ImageIoProcessorAdapter.subsampling(2000, 1500, 600));
    assertEquals(5, ImageIoProcessorAdapter.subsampling(6000, 6000, 600));
    assertEquals(2, ImageIoProcessorAdapter.subsampling(6000, 5000, 1200));
  }

  @Test
  @DisplayName("Foto grande ainda sai no tamanho certo depois da leitura reduzida")
  void largeImage() throws IOException {
    StoredImage out =
        processor.process(
            encode(image(5000, 4000, BufferedImage.TYPE_INT_RGB), "jpg"), ImageKind.PRODUCT);
    assertEquals(600, decode(out).getWidth());
  }

  @Test
  @DisplayName("Com o limite de processamentos simultâneos ocupado, o envio é recusado")
  void busy() throws Exception {
    ImageIoProcessorAdapter single = new ImageIoProcessorAdapter(1, 50);
    byte[] big = encode(image(4000, 4000, BufferedImage.TYPE_INT_RGB), "png");
    byte[] small = encode(image(100, 100, BufferedImage.TYPE_INT_RGB), "png");
    CountDownLatch started = new CountDownLatch(1);
    AtomicReference<Throwable> error = new AtomicReference<>();
    Thread worker =
        Thread.ofPlatform()
            .start(
                () -> {
                  started.countDown();
                  try {
                    for (int k = 0; k < 20; k++) {
                      single.process(big, ImageKind.COVER);
                    }
                  } catch (RuntimeException e) {
                    error.set(e);
                  }
                });
    assertTrue(started.await(5, TimeUnit.SECONDS));
    boolean refused = false;
    for (int k = 0; k < 200 && !refused && worker.isAlive(); k++) {
      try {
        single.process(small, ImageKind.LOGO);
      } catch (BusinessException e) {
        refused = e.getMessage().contains("Tente de novo");
      }
    }
    worker.join();
    assertTrue(refused);
    assertEquals(null, error.get());
  }

  @Test
  @DisplayName("EXIF com deslocamento absurdo não derruba o envio")
  void exifHugeOffset() {
    byte[] jpeg = exifJpeg(6, true);
    jpeg[16] = (byte) 0xFF;
    jpeg[17] = (byte) 0xFF;
    jpeg[18] = (byte) 0xFF;
    jpeg[19] = (byte) 0x7F;
    assertEquals(1, ImageIoProcessorAdapter.Exif.orientation(jpeg));
  }
}
