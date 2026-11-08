package com.binitech.pdv.adapters.outbound;

import com.binitech.pdv.domain.exception.BusinessException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SecretCipher {

  private static final Logger log = LoggerFactory.getLogger(SecretCipher.class);
  private static final String PREFIX = "v1:";
  private static final int IV_BYTES = 12;
  private static final int TAG_BITS = 128;

  private final SecretKey key;
  private final SecureRandom random = new SecureRandom();

  public SecretCipher(@Value("${pix.credentials-key:}") String base64Key) {
    this.key = parse(base64Key);
    if (key == null) {
      log.warn("PIX_CREDENTIALS_KEY ausente ou inválida: credenciais do Pix não podem ser salvas.");
    }
  }

  private static SecretKey parse(String base64Key) {
    if (base64Key == null || base64Key.isBlank()) {
      return null;
    }
    try {
      byte[] bytes = Base64.getDecoder().decode(base64Key.trim());
      return bytes.length == 32 ? new SecretKeySpec(bytes, "AES") : null;
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  public boolean isConfigured() {
    return key != null;
  }

  public String encrypt(String plain) {
    if (plain == null) {
      return null;
    }
    requireKey();
    try {
      byte[] iv = new byte[IV_BYTES];
      random.nextBytes(iv);
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
      byte[] encrypted = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
      ByteBuffer buffer = ByteBuffer.allocate(iv.length + encrypted.length);
      buffer.put(iv).put(encrypted);
      return PREFIX + Base64.getEncoder().encodeToString(buffer.array());
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException("Falha ao criptografar credencial.", e);
    }
  }

  public String decrypt(String stored) {
    if (stored == null) {
      return null;
    }
    requireKey();
    if (!stored.startsWith(PREFIX)) {
      throw new IllegalStateException("Credencial armazenada em formato desconhecido.");
    }
    try {
      byte[] all = Base64.getDecoder().decode(stored.substring(PREFIX.length()));
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, all, 0, IV_BYTES));
      byte[] plain = cipher.doFinal(all, IV_BYTES, all.length - IV_BYTES);
      return new String(plain, StandardCharsets.UTF_8);
    } catch (GeneralSecurityException | IllegalArgumentException e) {
      throw new IllegalStateException("Falha ao ler credencial criptografada.", e);
    }
  }

  private void requireKey() {
    if (key == null) {
      throw new BusinessException(
          "O servidor não está configurado para guardar credenciais do Pix. Avise o suporte.");
    }
  }
}
