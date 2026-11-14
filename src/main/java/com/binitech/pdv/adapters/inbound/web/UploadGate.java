package com.binitech.pdv.adapters.inbound.web;

import com.binitech.pdv.domain.exception.TooManyRequestsException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class UploadGate {

  static final String BUSY = "Muitas imagens sendo enviadas agora. Tente de novo em instantes.";
  static final String STORE_BUSY =
      "Sua loja já está enviando outras imagens. Espere terminar e tente de novo.";

  private final Semaphore slots;
  private final int perStore;
  private final long waitMillis;
  private final ConcurrentHashMap<String, Semaphore> stores = new ConcurrentHashMap<>();

  public UploadGate(
      @Value("${images.max-concurrent-uploads:4}") int maxConcurrent,
      @Value("${images.max-concurrent-uploads-per-store:2}") int perStore,
      @Value("${images.upload-wait:2s}") Duration wait) {
    this.slots = new Semaphore(Math.max(1, maxConcurrent), true);
    this.perStore = Math.max(1, perStore);
    this.waitMillis = Math.max(0, wait.toMillis());
  }

  public <T> T run(String storeId, Supplier<T> upload) {
    Semaphore store = stores.computeIfAbsent(String.valueOf(storeId), k -> new Semaphore(perStore));
    if (!store.tryAcquire()) {
      throw new TooManyRequestsException(STORE_BUSY);
    }
    try {
      acquireGlobal();
      try {
        return upload.get();
      } finally {
        slots.release();
      }
    } finally {
      store.release();
    }
  }

  private void acquireGlobal() {
    try {
      if (!slots.tryAcquire(waitMillis, TimeUnit.MILLISECONDS)) {
        throw new TooManyRequestsException(BUSY);
      }
    } catch (InterruptedException _) {
      Thread.currentThread().interrupt();
      throw new TooManyRequestsException(BUSY);
    }
  }
}
