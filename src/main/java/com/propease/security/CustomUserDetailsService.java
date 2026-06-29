package com.propease.security;

import com.propease.domain.User;
import com.propease.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
  private final UserRepository users;

  @Override
  public UserDetails loadUserByUsername(String email) {
    User u =
        users
            .findByEmailIgnoreCase(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    return org.springframework.security.core.userdetails.User.withUsername(u.getEmail())
        .password(u.getPassword())
        .disabled(!u.isEnabled())
        .authorities(u.getRoles().stream().map(r -> r.getName().name()).toArray(String[]::new))
        .build();
  }
}
