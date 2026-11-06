package com.binitech.pdv.domain;

import com.binitech.pdv.domain.exception.BusinessException;
import com.binitech.pdv.utils.enums.CashSessionStatus;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CashSession {

  public static final double MAX_AMOUNT = 1_000_000;

  private String id;
  private String tenantId;
  private String userId;
  private String operatorName;
  private LocalDateTime openedAt;
  private double openingAmount;
  private CashSessionStatus status = CashSessionStatus.OPEN;
  private List<CashMovement> movements = new ArrayList<>();
  private LocalDateTime closedAt;
  private String closedByUserId;
  private Double countedAmount;
  private Double difference;
  private String notes;
  private CashSessionSummary closingSummary;

  public static double requireAmount(Double amount, boolean allowZero) {
    if (amount == null || amount.isNaN() || amount < 0) {
      throw new BusinessException("Informe um valor válido.");
    }
    if (!allowZero && Money.compare(amount, 0) <= 0) {
      throw new BusinessException("Informe um valor maior que zero.");
    }
    if (amount > MAX_AMOUNT) {
      throw new BusinessException("Valor acima do limite permitido.");
    }
    return Money.round(amount);
  }

  public boolean isOpen() {
    return status == CashSessionStatus.OPEN;
  }

  public void close(
      String userId, LocalDateTime now, Double counted, String notes, CashSessionSummary summary) {
    if (!isOpen()) {
      throw new BusinessException("Este caixa já foi fechado.");
    }
    Double countedValue = counted == null ? null : requireAmount(counted, true);
    this.status = CashSessionStatus.CLOSED;
    this.closedAt = now;
    this.closedByUserId = userId;
    this.countedAmount = countedValue;
    this.difference =
        countedValue == null ? null : Money.round(countedValue - summary.expectedCash());
    this.notes = notes == null || notes.isBlank() ? null : notes.trim();
    this.closingSummary = summary;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getTenantId() {
    return tenantId;
  }

  public void setTenantId(String tenantId) {
    this.tenantId = tenantId;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getOperatorName() {
    return operatorName;
  }

  public void setOperatorName(String operatorName) {
    this.operatorName = operatorName;
  }

  public LocalDateTime getOpenedAt() {
    return openedAt;
  }

  public void setOpenedAt(LocalDateTime openedAt) {
    this.openedAt = openedAt;
  }

  public double getOpeningAmount() {
    return openingAmount;
  }

  public void setOpeningAmount(double openingAmount) {
    this.openingAmount = openingAmount;
  }

  public CashSessionStatus getStatus() {
    return status;
  }

  public void setStatus(CashSessionStatus status) {
    this.status = status;
  }

  public List<CashMovement> getMovements() {
    return movements;
  }

  public void setMovements(List<CashMovement> movements) {
    this.movements = movements == null ? new ArrayList<>() : new ArrayList<>(movements);
  }

  public LocalDateTime getClosedAt() {
    return closedAt;
  }

  public void setClosedAt(LocalDateTime closedAt) {
    this.closedAt = closedAt;
  }

  public String getClosedByUserId() {
    return closedByUserId;
  }

  public void setClosedByUserId(String closedByUserId) {
    this.closedByUserId = closedByUserId;
  }

  public Double getCountedAmount() {
    return countedAmount;
  }

  public void setCountedAmount(Double countedAmount) {
    this.countedAmount = countedAmount;
  }

  public Double getDifference() {
    return difference;
  }

  public void setDifference(Double difference) {
    this.difference = difference;
  }

  public String getNotes() {
    return notes;
  }

  public void setNotes(String notes) {
    this.notes = notes;
  }

  public CashSessionSummary getClosingSummary() {
    return closingSummary;
  }

  public void setClosingSummary(CashSessionSummary closingSummary) {
    this.closingSummary = closingSummary;
  }
}
