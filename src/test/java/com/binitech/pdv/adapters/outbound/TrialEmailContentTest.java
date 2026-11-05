package com.binitech.pdv.adapters.outbound;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.binitech.pdv.domain.EmailEvent;
import com.binitech.pdv.domain.EmailEvent.EmailType;
import com.binitech.pdv.domain.TrialEmail;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TrialEmailContentTest {

  @Test
  @DisplayName("Cada etapa do teste tem assunto, texto e link próprios")
  void everyStepHasContent() {
    for (TrialEmail step : TrialEmail.values()) {
      EmailEvent event = EmailEvent.trial("a@b.com", "Loja", step);
      TrialEmailContent content = TrialEmailContent.of(event.type(), "https://app");
      assertNotNull(content, step.name());
      assertTrue(content.link().startsWith("https://app/"));
      assertTrue(content.body().contains("<p"));
    }
    assertEquals(
        "https://app/billing", TrialEmailContent.of(EmailType.TRIAL_ENDING, "https://app").link());
    assertNull(TrialEmailContent.of(EmailType.WELCOME, "https://app"));
  }
}
