package com.propease.service;

import com.propease.domain.Enums;
import com.propease.domain.User;
import com.propease.dto.AuthDtos.AuthResponse;
import com.propease.dto.AuthDtos.LoginRequest;
import com.propease.dto.AuthDtos.RefreshRequest;
import com.propease.dto.AuthDtos.RegisterRequest;
import com.propease.exception.BusinessException;
import com.propease.repository.RoleRepository;
import com.propease.repository.UserRepository;
import com.propease.security.JwtTokenProvider;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {
  private final UserRepository users;
  private final RoleRepository roles;
  private final PasswordEncoder encoder;
  private final AuthenticationManager authenticationManager;
  private final JwtTokenProvider tokens;

  @Transactional
  public AuthResponse register(RegisterRequest r) {
    if (users.existsByEmailIgnoreCase(r.email()))
      throw new BusinessException("Email is already registered");
    var assigned =
        r.roles().stream()
            .map(x -> Enums.RoleName.valueOf("ROLE_" + x))
            .map(
                x ->
                    roles
                        .findByName(x)
                        .orElseThrow(() -> new IllegalStateException("Role seed missing: " + x)))
            .collect(Collectors.toSet());
    users.save(
        User.builder()
            .fullName(r.fullName().trim())
            .email(r.email().trim().toLowerCase())
            .password(encoder.encode(r.password()))
            .roles(assigned)
            .build());
    return authenticate(r.email(), r.password());
  }

  public AuthResponse login(LoginRequest r) {
    return authenticate(r.email(), r.password());
  }

  public AuthResponse refresh(RefreshRequest r) {
    String email = tokens.subject(r.refreshToken(), "REFRESH");
    User user =
        users
            .findByEmailIgnoreCase(email)
            .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));
    Authentication a =
        UsernamePasswordAuthenticationToken.authenticated(
            user.getEmail(),
            null,
            user.getRoles().stream()
                .map(
                    x ->
                        new org.springframework.security.core.authority.SimpleGrantedAuthority(
                            x.getName().name()))
                .toList());
    return response(a, user);
  }

  private AuthResponse authenticate(String email, String password) {
    Authentication a =
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(email, password));
    User u = users.findByEmailIgnoreCase(email).orElseThrow();
    return response(a, u);
  }

  private AuthResponse response(Authentication a, User u) {
    return new AuthResponse(
        tokens.accessToken(a),
        tokens.refreshToken(a),
        "Bearer",
        tokens.accessExpirationSeconds(),
        u.getRoles().stream()
            .map(x -> x.getName().name().substring(5))
            .collect(Collectors.toSet()));
  }
}
