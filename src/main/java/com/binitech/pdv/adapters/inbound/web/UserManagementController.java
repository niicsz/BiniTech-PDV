package com.binitech.pdv.adapters.inbound.web;

import com.binitech.pdv.application.ports.inbound.UserManagementUseCasePort;
import com.binitech.pdv.domain.User;
import com.binitech.pdv.utils.enums.Role;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserManagementController {

  private final UserManagementUseCasePort userManagementUseCase;
  private final AuthenticatedUserProvider authenticatedUserProvider;

  public UserManagementController(
      UserManagementUseCasePort userManagementUseCase,
      AuthenticatedUserProvider authenticatedUserProvider) {
    this.userManagementUseCase = userManagementUseCase;
    this.authenticatedUserProvider = authenticatedUserProvider;
  }

  @GetMapping
  public ResponseEntity<List<UserDTO>> listUsers() {
    String actorUserId = authenticatedUserProvider.getUserId();
    Role actorRole = authenticatedUserProvider.getUserRole();
    String tenantId = authenticatedUserProvider.getTenantId();
    return ResponseEntity.ok(
        userManagementUseCase.listUsers(tenantId, actorRole).stream()
            .map(user -> toDto(user, actorUserId, actorRole))
            .toList());
  }

  @GetMapping("/{id}")
  public ResponseEntity<UserDTO> getUser(@PathVariable String id) {
    String actorUserId = authenticatedUserProvider.getUserId();
    Role actorRole = authenticatedUserProvider.getUserRole();
    User user =
        userManagementUseCase.getUser(id, authenticatedUserProvider.getTenantId(), actorRole);
    return ResponseEntity.ok(toDto(user, actorUserId, actorRole));
  }

  @PostMapping
  public ResponseEntity<UserDTO> createUser(@Valid @RequestBody CreateUserRequest request) {
    Role actorRole = authenticatedUserProvider.getUserRole();
    User saved =
        userManagementUseCase.createUser(
            request.name(),
            request.email(),
            request.password(),
            request.role(),
            authenticatedUserProvider.getTenantId(),
            actorRole);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(toDto(saved, authenticatedUserProvider.getUserId(), actorRole));
  }

  @PatchMapping("/{id}/status")
  public ResponseEntity<UserDTO> updateStatus(
      @PathVariable String id, @Valid @RequestBody UpdateUserStatusRequest request) {
    String actorUserId = authenticatedUserProvider.getUserId();
    Role actorRole = authenticatedUserProvider.getUserRole();
    User saved =
        userManagementUseCase.updateStatus(
            id, request.active(), actorUserId, authenticatedUserProvider.getTenantId(), actorRole);
    return ResponseEntity.ok(toDto(saved, actorUserId, actorRole));
  }

  @PatchMapping("/{id}/role")
  public ResponseEntity<UserDTO> updateRole(
      @PathVariable String id, @Valid @RequestBody UpdateUserRoleRequest request) {
    String actorUserId = authenticatedUserProvider.getUserId();
    Role actorRole = authenticatedUserProvider.getUserRole();
    User saved =
        userManagementUseCase.updateRole(
            id, request.role(), actorUserId, authenticatedUserProvider.getTenantId(), actorRole);
    return ResponseEntity.ok(toDto(saved, actorUserId, actorRole));
  }

  private UserDTO toDto(User user, String actorUserId, Role actorRole) {
    boolean currentUser = user.getId().equals(actorUserId);
    boolean manageable =
        !currentUser
            && (actorRole == Role.ADMIN
                ? user.getRole() == Role.TENANT_ADMIN || user.getRole() == Role.OPERATOR
                : user.getRole() == Role.OPERATOR);
    return new UserDTO(
        user.getId(),
        user.getName() == null || user.getName().isBlank() ? user.getUsername() : user.getName(),
        user.getEmail(),
        user.getUsername(),
        user.getRole(),
        user.isActive(),
        currentUser,
        manageable,
        user.getCreatedAt(),
        user.getUpdatedAt());
  }

  public record CreateUserRequest(
      @NotBlank @Size(max = 120) String name,
      @NotBlank @Email @Size(max = 254) String email,
      @NotBlank @Size(min = 6, max = 128) String password,
      @NotNull Role role) {}

  public record UpdateUserStatusRequest(@NotNull Boolean active) {}

  public record UpdateUserRoleRequest(@NotNull Role role) {}

  public record UserDTO(
      String id,
      String name,
      String email,
      String username,
      Role role,
      boolean active,
      boolean currentUser,
      boolean manageable,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {}
}
