package com.binitech.pdv.adapters.inbound.web;

import jakarta.servlet.http.HttpServletRequest;
import java.util.regex.Pattern;

final class ClientIpResolver {

  private static final Pattern IP = Pattern.compile("[0-9A-Fa-f.:]{2,45}");

  private ClientIpResolver() {}

  static String resolve(HttpServletRequest request) {
    String realIp = request.getHeader("X-Real-IP");
    if (realIp != null && IP.matcher(realIp.trim()).matches()) {
      return realIp.trim();
    }
    return request.getRemoteAddr();
  }
}
