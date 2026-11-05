package com.binitech.pdv.application.ports.inbound;

import java.time.LocalDate;

public interface TrialNurtureUseCasePort {

  int sendDueEmails(LocalDate today);
}
