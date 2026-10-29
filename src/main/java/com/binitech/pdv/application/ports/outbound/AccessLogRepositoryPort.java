package com.binitech.pdv.application.ports.outbound;

import com.binitech.pdv.domain.AccessLog;

public interface AccessLogRepositoryPort {
  void save(AccessLog accessLog);
}
