package com.binitech.pdv.adapters.outbound.persistence;

import com.binitech.pdv.adapters.outbound.persistence.document.UserDocument;
import com.binitech.pdv.adapters.outbound.persistence.repository.SpringDataUserRepository;
import com.binitech.pdv.application.ports.outbound.UserRepositoryPort;
import com.binitech.pdv.domain.User;
import com.binitech.pdv.utils.Enum.Role;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class UserRepositoryAdapter implements UserRepositoryPort {

  private final SpringDataUserRepository repository;

  public UserRepositoryAdapter(SpringDataUserRepository repository) {
    this.repository = repository;
  }

  @Override
  public User save(User user) {
    UserDocument doc = toDocument(user);
    UserDocument saved = repository.save(doc);
    return toDomain(saved);
  }

  @Override
  public Optional<User> findById(String id) {
    return repository.findById(id).map(this::toDomain);
  }

  @Override
  public Optional<User> findByUsername(String username) {
    return repository.findByUsername(username).map(this::toDomain);
  }

  @Override
  public boolean existsByUsername(String username) {
    return repository.existsByUsername(username);
  }

  private UserDocument toDocument(User user) {
    return UserDocument.builder()
        .id(user.getId())
        .username(user.getUsername())
        .password(user.getPassword())
        .role(user.getRole().name())
        .build();
  }

  private User toDomain(UserDocument doc) {
    return new User(doc.getId(), doc.getUsername(), doc.getPassword(), Role.valueOf(doc.getRole()));
  }
}
