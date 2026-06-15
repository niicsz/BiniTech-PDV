package com.binitech.pdv.domain;

import com.binitech.pdv.utils.Enum.Role;
import java.util.Objects;

public class User {

  private String id;
  private String username;
  private String password;
  private Role role;
  private String tenantId;

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
        + "'}";
  }
}
