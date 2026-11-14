package com.binitech.pdv.adapters.outbound.image;

import com.binitech.pdv.application.ports.outbound.ImageProcessorPort;
import com.binitech.pdv.domain.StoredImage;
import com.binitech.pdv.domain.exception.BusinessException;
import com.binitech.pdv.utils.enums.ImageKind;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;

public class ImageIoProcessorAdapter implements ImageProcessorPort {

  private static final long MAX_PIXELS = 40_000_000L;
  private static final int MAX_SIDE = 12_000;
  private static final int MIN_SIDE = 64;
  private static final float JPEG_QUALITY = 0.85f;
  private static final String UNSUPPORTED = "Envie uma imagem JPG, PNG ou WebP.";
  private static final String UNREADABLE = "Não foi possível ler a imagem. Tente outro arquivo.";
  private static final String BUSY = "Muitas imagens sendo processadas agora. Tente de novo.";

  private final Semaphore slots;
  private final long waitMillis;

  private record Spec(int width, int height, boolean fill, boolean png) {
    int largestSide() {
      return Math.max(width, height);
    }
  }

  public ImageIoProcessorAdapter() {
    this(2, 10_000);
  }

  ImageIoProcessorAdapter(int concurrent, long waitMillis) {
    this.slots = new Semaphore(concurrent, true);
    this.waitMillis = waitMillis;
  }

  @Override
  public StoredImage process(byte[] content, ImageKind kind) {
    Format format = Format.detect(content);
    if (format == null) {
      throw new BusinessException(UNSUPPORTED);
    }
    acquire();
    try {
      return render(content, format, spec(kind));
    } finally {
      slots.release();
    }
  }

  private void acquire() {
    try {
      if (!slots.tryAcquire(waitMillis, TimeUnit.MILLISECONDS)) {
        throw new BusinessException(BUSY);
      }
    } catch (InterruptedException interrupted) {
      Thread.currentThread().interrupt();
      throw new BusinessException(BUSY);
    }
  }

  private static StoredImage render(byte[] content, Format format, Spec spec) {
    BufferedImage image =
        orient(
            read(content, spec.largestSide()),
            format == Format.JPEG ? Exif.orientation(content) : 1);
    BufferedImage resized =
        spec.fill()
            ? fill(image, spec.width(), spec.height(), spec.png())
            : fit(image, spec.width(), spec.height(), spec.png());
    return spec.png()
        ? new StoredImage(png(resized), "image/png")
        : new StoredImage(jpeg(resized), "image/jpeg");
  }

  private static Spec spec(ImageKind kind) {
    return switch (kind) {
      case LOGO -> new Spec(400, 400, false, true);
      case COVER -> new Spec(1200, 400, true, false);
      case PRODUCT -> new Spec(600, 600, true, false);
    };
  }

  static int subsampling(int width, int height, int target) {
    return Math.max(1, Math.min(width, height) / (2 * target));
  }

  private static BufferedImage read(byte[] content, int target) {
    try (ImageInputStream input =
        ImageIO.createImageInputStream(new ByteArrayInputStream(content))) {
      Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
      if (!readers.hasNext()) {
        throw new BusinessException(UNSUPPORTED);
      }
      ImageReader reader = readers.next();
      try {
        reader.setInput(input, true, true);
        int width = reader.getWidth(0);
        int height = reader.getHeight(0);
        if (width > MAX_SIDE || height > MAX_SIDE || (long) width * height > MAX_PIXELS) {
          throw new BusinessException("A imagem tem resolução grande demais. Envie uma menor.");
        }
        if (width < MIN_SIDE || height < MIN_SIDE) {
          throw new BusinessException(
              "A imagem é pequena demais: precisa de pelo menos " + MIN_SIDE + " px de lado.");
        }
        ImageReadParam param = reader.getDefaultReadParam();
        int step = subsampling(width, height, target);
        param.setSourceSubsampling(step, step, 0, 0);
        BufferedImage image = reader.read(0, param);
        if (image == null) {
          throw new BusinessException(UNREADABLE);
        }
        return image;
      } finally {
        reader.dispose();
      }
    } catch (IOException | RuntimeException exception) {
      if (exception instanceof BusinessException business) {
        throw business;
      }
      throw new BusinessException(UNREADABLE);
    }
  }

  static BufferedImage orient(BufferedImage image, int orientation) {
    if (orientation < 2 || orientation > 8) {
      return image;
    }
    int w = image.getWidth();
    int h = image.getHeight();
    boolean swap = orientation >= 5;
    AffineTransform t = new AffineTransform();
    switch (orientation) {
      case 2 -> t.setTransform(-1, 0, 0, 1, w, 0);
      case 3 -> t.setTransform(-1, 0, 0, -1, w, h);
      case 4 -> t.setTransform(1, 0, 0, -1, 0, h);
      case 5 -> t.setTransform(0, 1, 1, 0, 0, 0);
      case 6 -> t.setTransform(0, 1, -1, 0, h, 0);
      case 7 -> t.setTransform(0, -1, -1, 0, h, w);
      default -> t.setTransform(0, -1, 1, 0, 0, w);
    }
    BufferedImage out = new BufferedImage(swap ? h : w, swap ? w : h, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = out.createGraphics();
    try {
      g.drawImage(image, t, null);
    } finally {
      g.dispose();
    }
    return out;
  }

  private static BufferedImage fit(BufferedImage image, int maxW, int maxH, boolean alpha) {
    double scale =
        Math.min(
            1.0, Math.min((double) maxW / image.getWidth(), (double) maxH / image.getHeight()));
    int w = Math.max(1, (int) Math.round(image.getWidth() * scale));
    int h = Math.max(1, (int) Math.round(image.getHeight() * scale));
    return draw(downscale(image, w, h), new Rectangle(0, 0, w, h), new Dimension(w, h), alpha);
  }

  private static BufferedImage fill(BufferedImage image, int w, int h, boolean alpha) {
    double scale = Math.max((double) w / image.getWidth(), (double) h / image.getHeight());
    int scaledW = Math.max(w, (int) Math.ceil(image.getWidth() * scale));
    int scaledH = Math.max(h, (int) Math.ceil(image.getHeight() * scale));
    BufferedImage scaled = downscale(image, scaledW, scaledH);
    int x = (scaledW - w) / 2;
    int y = (scaledH - h) / 2;
    return draw(scaled, new Rectangle(-x, -y, scaledW, scaledH), new Dimension(w, h), alpha);
  }

  private static BufferedImage downscale(BufferedImage image, int targetW, int targetH) {
    BufferedImage current = image;
    int w = image.getWidth();
    int h = image.getHeight();
    while (w / 2 >= targetW && h / 2 >= targetH) {
      w /= 2;
      h /= 2;
      current = draw(current, new Rectangle(0, 0, w, h), new Dimension(w, h), true);
    }
    return current;
  }

  private static BufferedImage draw(
      BufferedImage source, Rectangle target, Dimension canvas, boolean alpha) {
    int canvasW = canvas.width;
    int canvasH = canvas.height;
    BufferedImage out =
        new BufferedImage(
            canvasW, canvasH, alpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
    Graphics2D g = out.createGraphics();
    try {
      if (!alpha) {
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, canvasW, canvasH);
      }
      g.setRenderingHint(
          RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
      g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g.drawImage(source, target.x, target.y, target.width, target.height, null);
    } finally {
      g.dispose();
    }
    return out;
  }

  private static byte[] png(BufferedImage image) {
    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      ImageIO.write(image, "png", out);
      return out.toByteArray();
    } catch (IOException exception) {
      throw new IllegalStateException("Falha ao gerar PNG", exception);
    }
  }

  private static byte[] jpeg(BufferedImage image) {
    ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
    try (ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageOutputStream stream = ImageIO.createImageOutputStream(out)) {
      ImageWriteParam param = writer.getDefaultWriteParam();
      param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
      param.setCompressionQuality(JPEG_QUALITY);
      writer.setOutput(stream);
      writer.write(null, new IIOImage(image, null, null), param);
      stream.flush();
      return out.toByteArray();
    } catch (IOException exception) {
      throw new IllegalStateException("Falha ao gerar JPEG", exception);
    } finally {
      writer.dispose();
    }
  }

  enum Format {
    JPEG,
    PNG,
    WEBP;

    static Format detect(byte[] b) {
      if (b == null || b.length < 12) {
        return null;
      }
      if ((b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
        return JPEG;
      }
      if ((b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') {
        return PNG;
      }
      if (b[0] == 'R'
          && b[1] == 'I'
          && b[2] == 'F'
          && b[3] == 'F'
          && b[8] == 'W'
          && b[9] == 'E'
          && b[10] == 'B'
          && b[11] == 'P') {
        return WEBP;
      }
      return null;
    }
  }

  static final class Exif {

    private Exif() {}

    static int orientation(byte[] b) {
      int i = 2;
      while (i + 4 <= b.length) {
        int length = segmentLength(b, i);
        if (length < 0) {
          return 1;
        }
        if ((b[i + 1] & 0xFF) == 0xE1 && isExif(b, i + 4)) {
          return fromTiff(b, i + 10, Math.min(b.length, i + 2 + length));
        }
        i += 2 + length;
      }
      return 1;
    }

    private static int segmentLength(byte[] b, int at) {
      int marker = b[at + 1] & 0xFF;
      boolean invalid = (b[at] & 0xFF) != 0xFF || marker == 0xDA || marker == 0xD9;
      int length = ((b[at + 2] & 0xFF) << 8) | (b[at + 3] & 0xFF);
      return invalid || length < 2 ? -1 : length;
    }

    private static boolean isExif(byte[] b, int at) {
      return at + 6 <= b.length
          && b[at] == 'E'
          && b[at + 1] == 'x'
          && b[at + 2] == 'i'
          && b[at + 3] == 'f'
          && b[at + 4] == 0
          && b[at + 5] == 0;
    }

    private static int fromTiff(byte[] b, int start, int end) {
      if (start + 8 > end) {
        return 1;
      }
      boolean little = b[start] == 'I' && b[start + 1] == 'I';
      boolean big = b[start] == 'M' && b[start + 1] == 'M';
      if (!little && !big) {
        return 1;
      }
      long ifd = read32(b, start + 4, little);
      if (ifd < 8 || ifd > end - start - 2L) {
        return 1;
      }
      int dir = start + (int) ifd;
      int entries = read16(b, dir, little);
      for (int e = 0; e < entries; e++) {
        int entry = dir + 2 + e * 12;
        if (entry + 12 > end) {
          return 1;
        }
        if (read16(b, entry, little) == 0x0112) {
          int value = read16(b, entry + 8, little);
          return value >= 1 && value <= 8 ? value : 1;
        }
      }
      return 1;
    }

    private static int read16(byte[] b, int at, boolean little) {
      int a = b[at] & 0xFF;
      int c = b[at + 1] & 0xFF;
      return little ? (c << 8) | a : (a << 8) | c;
    }

    private static long read32(byte[] b, int at, boolean little) {
      long v = 0;
      for (int k = 0; k < 4; k++) {
        int shift = little ? 8 * k : 8 * (3 - k);
        v |= (long) (b[at + k] & 0xFF) << shift;
      }
      return v;
    }
  }
}
