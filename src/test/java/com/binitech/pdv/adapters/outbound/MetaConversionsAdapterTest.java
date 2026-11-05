package com.binitech.pdv.adapters.outbound;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MetaConversionsAdapterTest {

  @Test
  @DisplayName("Só liga com ID numérico do Pixel e token preenchidos")
  void enabledOnlyWhenConfigured() {
    assertFalse(new MetaConversionsAdapter("", "", "http://localhost").enabled());
    assertFalse(new MetaConversionsAdapter("123456789", "", "http://localhost").enabled());
    assertFalse(new MetaConversionsAdapter("abc", "token", "http://localhost").enabled());
    assertTrue(new MetaConversionsAdapter("123456789", "token", "http://localhost").enabled());
  }

  @Test
  @DisplayName("Dados pessoais vão para a Meta só como hash SHA-256")
  void hashesPersonalData() {
    assertEquals(
        "973dfe463ec85785f5f95af5ba3906eedb2d931c24e69824a89ea65dba4e813b",
        MetaConversionsAdapter.sha256("test@example.com"));
  }
}
