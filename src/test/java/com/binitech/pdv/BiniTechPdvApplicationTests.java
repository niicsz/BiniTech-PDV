package com.binitech.pdv;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
    properties = {
      "security.pepper=test-pepper-for-unit-tests",
      "admin.password=testAdminPass123"
    })
class BiniTechPdvApplicationTests {

  @Test
  void contextLoads() {}
}
