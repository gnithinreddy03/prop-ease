package com.propease.service;

import com.propease.domain.User;
import com.propease.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CurrentUserService {
  private final UserRepository users;

  public User get() {
    String email = SecurityContextHolder.getContext().getAuthentication().getName();
    return users
        .findByEmailIgnoreCase(email)
        .orElseThrow(() -> new EntityNotFoundException("Authenticated user not found"));
  }
}
