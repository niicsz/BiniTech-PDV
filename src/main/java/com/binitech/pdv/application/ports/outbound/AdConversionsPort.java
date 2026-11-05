package com.binitech.pdv.application.ports.outbound;

import java.math.BigDecimal;

public interface AdConversionsPort {

  void sendSignup(SignupConversion conversion);

  record SignupConversion(
      String eventId,
      String email,
      String phone,
      String ip,
      String userAgent,
      String fbp,
      String fbc,
      String sourceUrl,
      BigDecimal monthlyValue) {}
}
