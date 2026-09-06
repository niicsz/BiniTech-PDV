package com.binitech.pdv.domain;

import com.binitech.pdv.utils.enums.Role;
import java.time.LocalDateTime;
import java.util.Objects;

public class User {

  private String id;
  private String username;
  private String name;
  private String email;
  private String password;
  private Role role;
  private String tenantId;
  private Boolean active;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public User() {}

  public User(String id, String username, String password, Role role) {
    this(id, username, password, role, null);
  }

  public User(String id, String username, String password, Role role, String tenantId) {
    this.id = id;
    this.username = username;
    this.password = password;
    this.role = role;
    this.tenantId = tenantId;
    this.active = true;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  @com.fasterxml.jackson.annotation.JsonIgnore
  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public Role getRole() {
    return role;
  }

  public void setRole(Role role) {
    this.role = role;
  }

  public String getTenantId() {
    return tenantId;
  }

  public void setTenantId(String tenantId) {
    this.tenantId = tenantId;
  }

  /** Usuários anteriores à inclusão deste campo permanecem ativos por compatibilidade. */
  public boolean isActive() {
    return active == null || active;
  }

  public Boolean getActive() {
    return active;
  }

  public void setActive(Boolean active) {
    this.active = active;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    User user = (User) o;
    return Objects.equals(id, user.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public String toString() {
    return "User{id='"
        + id
        + "', username='"
        + username
        + "', role="
        + role
        + ", tenantId='"
        + tenantId
        + "', active="
        + isActive()
        + "}";
  }
}
