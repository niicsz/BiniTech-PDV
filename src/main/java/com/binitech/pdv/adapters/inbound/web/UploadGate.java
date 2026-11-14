package com.binitech.pdv.adapters.inbound.web;

import com.binitech.pdv.domain.exception.TooManyRequestsException;
import java.time.Duration;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class UploadGate {

  static final String BUSY = "Muitas imagens sendo enviadas agora. Tente de novo em instantes.";

  private final Semaphore slots;
  private final long waitMillis;

  public UploadGate(
      @Value("${images.max-concurrent-uploads:4}") int maxConcurrent,
      @Value("${images.upload-wait:2s}") Duration wait) {
    this.slots = new Semaphore(Math.max(1, maxConcurrent), true);
    this.waitMillis = Math.max(0, wait.toMillis());
  }

  public <T> T run(Supplier<T> upload) {
    try {
      if (!slots.tryAcquire(waitMillis, TimeUnit.MILLISECONDS)) {
        throw new TooManyRequestsException(BUSY);
      }
    } catch (InterruptedException _) {
      Thread.currentThread().interrupt();
      throw new TooManyRequestsException(BUSY);
    }
    try {
      return upload.get();
    } finally {
      slots.release();
    }
  }
}
