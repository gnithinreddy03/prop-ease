package com.propease.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.propease.domain.Enums;
import com.propease.domain.Role;
import com.propease.domain.User;
import com.propease.repository.UserRepository;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

class CustomUserDetailsServiceTest {
  @Test
  void mapsMultipleRoles() {
    UserRepository repo = mock(UserRepository.class);
    Role owner = Role.builder().name(Enums.RoleName.ROLE_OWNER).build();
    Role tenant = Role.builder().name(Enums.RoleName.ROLE_TENANT).build();
    when(repo.findByEmailIgnoreCase("a@b.com"))
        .thenReturn(
            Optional.of(
                User.builder()
                    .email("a@b.com")
                    .password("hash")
                    .enabled(true)
                    .roles(Set.of(owner, tenant))
                    .build()));
    var result = new CustomUserDetailsService(repo).loadUserByUsername("a@b.com");
    assertThat(result.getAuthorities())
        .extracting("authority")
        .containsExactlyInAnyOrder("ROLE_OWNER", "ROLE_TENANT");
  }

  @Test
  void missingUserFails() {
    UserRepository repo = mock(UserRepository.class);
    when(repo.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());
    assertThatThrownBy(() -> new CustomUserDetailsService(repo).loadUserByUsername("x@y.com"))
        .isInstanceOf(UsernameNotFoundException.class);
  }
}
