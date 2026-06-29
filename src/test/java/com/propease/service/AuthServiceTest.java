package com.propease.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.propease.domain.Enums;
import com.propease.domain.Role;
import com.propease.domain.User;
import com.propease.dto.AuthDtos.AuthResponse;
import com.propease.dto.AuthDtos.RegisterRequest;
import com.propease.exception.BusinessException;
import com.propease.repository.RoleRepository;
import com.propease.repository.UserRepository;
import com.propease.security.JwtTokenProvider;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthServiceTest {
  @Mock UserRepository users;
  @Mock RoleRepository roles;
  @Mock PasswordEncoder encoder;
  @Mock AuthenticationManager manager;
  @Mock JwtTokenProvider tokens;
  @InjectMocks AuthService service;

  @BeforeEach
  void init() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void rejectsDuplicateRegistration() {
    when(users.existsByEmailIgnoreCase("a@b.com")).thenReturn(true);
    assertThatThrownBy(
            () ->
                service.register(
                    new RegisterRequest("John", "a@b.com", "Password1", Set.of("OWNER"))))
        .isInstanceOf(BusinessException.class);
  }

  @Test
  void registersAllRolesAndReturnsTokens() {
    Role owner = Role.builder().name(Enums.RoleName.ROLE_OWNER).build();
    when(users.existsByEmailIgnoreCase(anyString())).thenReturn(false);
    when(roles.findByName(Enums.RoleName.ROLE_OWNER)).thenReturn(Optional.of(owner));
    when(encoder.encode(anyString())).thenReturn("hash");
    var auth = UsernamePasswordAuthenticationToken.authenticated("a@b.com", null, List.of());
    when(manager.authenticate(any())).thenReturn(auth);
    User user = User.builder().email("a@b.com").roles(Set.of(owner)).build();
    when(users.findByEmailIgnoreCase(anyString())).thenReturn(Optional.of(user));
    when(tokens.accessToken(auth)).thenReturn("access");
    when(tokens.refreshToken(auth)).thenReturn("refresh");
    when(tokens.accessExpirationSeconds()).thenReturn(900L);
    AuthResponse result =
        service.register(new RegisterRequest("John", "a@b.com", "Password1", Set.of("OWNER")));
    assertThat(result.accessToken()).isEqualTo("access");
    assertThat(result.roles()).containsExactly("OWNER");
    verify(users).save(any(User.class));
  }
}
