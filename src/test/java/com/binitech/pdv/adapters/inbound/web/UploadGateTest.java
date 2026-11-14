package com.binitech.pdv.adapters.inbound.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.binitech.pdv.adapters.inbound.web.generated.model.ErrorDTO;
import com.binitech.pdv.domain.exception.TooManyRequestsException;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class UploadGateTest {

  @Test
  @DisplayName("Com todas as vagas ocupadas, o envio seguinte recebe 429 sem ler o arquivo")
  void rejectsWhenFull() throws Exception {
    UploadGate gate = new UploadGate(1, 2, Duration.ofMillis(20));
    CountDownLatch inside = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    Thread holder =
        Thread.ofPlatform()
            .start(
                () ->
                    gate.run(
                        "t1",
                        () -> {
                          inside.countDown();
                          try {
                            return release.await(5, TimeUnit.SECONDS);
                          } catch (InterruptedException _) {
                            Thread.currentThread().interrupt();
                            return false;
                          }
                        }));
    assertTrue(inside.await(5, TimeUnit.SECONDS));
    boolean[] read = {false};
    assertThrows(
        TooManyRequestsException.class,
        () ->
            gate.run(
                "t1",
                () -> {
                  read[0] = true;
                  return 1;
                }));
    assertTrue(!read[0]);
    release.countDown();
    holder.join();
    assertEquals(2, gate.run("t1", () -> 2));
  }

  @Test
  @DisplayName("A vaga é devolvida mesmo quando o envio falha")
  void releasesOnFailure() {
    UploadGate gate = new UploadGate(1, 1, Duration.ZERO);
    assertThrows(
        IllegalStateException.class,
        () ->
            gate.run(
                "t1",
                () -> {
                  throw new IllegalStateException("falhou");
                }));
    assertEquals(3, gate.run("t1", () -> 3));
  }

  @Test
  @DisplayName("Excesso de envios vira 429 com Retry-After")
  void handlerReturns429() {
    ResponseEntity<ErrorDTO> response =
        new GlobalExceptionHandler()
            .handleTooManyRequests(new TooManyRequestsException(UploadGate.BUSY));
    assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.getStatusCode());
    assertEquals("5", response.getHeaders().getFirst("Retry-After"));
    assertEquals(UploadGate.BUSY, response.getBody().getMessage());
  }

  @Test
  @DisplayName("Uma loja no limite recebe 429 enquanto outra loja continua enviando")
  void perStoreLimit() throws Exception {
    UploadGate gate = new UploadGate(4, 1, Duration.ofMillis(20));
    CountDownLatch inside = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    Thread holder =
        Thread.ofPlatform()
            .start(
                () ->
                    gate.run(
                        "loja-a",
                        () -> {
                          inside.countDown();
                          try {
                            return release.await(5, TimeUnit.SECONDS);
                          } catch (InterruptedException _) {
                            Thread.currentThread().interrupt();
                            return false;
                          }
                        }));
    assertTrue(inside.await(5, TimeUnit.SECONDS));
    TooManyRequestsException error =
        assertThrows(TooManyRequestsException.class, () -> gate.run("loja-a", () -> 1));
    assertEquals(UploadGate.STORE_BUSY, error.getMessage());
    assertEquals(4, gate.run("loja-b", () -> 4));
    release.countDown();
    holder.join();
    assertEquals(5, gate.run("loja-a", () -> 5));
  }
}
