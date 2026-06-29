package com.propease.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

class JwtTokenProviderTest {
  private JwtTokenProvider provider;

  @BeforeEach
  void setUp() {
    provider = new JwtTokenProvider("0123456789012345678901234567890123456789", 60000, 120000);
    provider.init();
  }

  @Test
  void createsAndValidatesTypedTokens() {
    var auth =
        UsernamePasswordAuthenticationToken.authenticated(
            "user@example.com", null, java.util.List.of());
    String access = provider.accessToken(auth);
    String refresh = provider.refreshToken(auth);
    assertThat(provider.validAccess(access)).isTrue();
    assertThat(provider.subject(access, "ACCESS")).isEqualTo("user@example.com");
    assertThat(provider.subject(refresh, "REFRESH")).isEqualTo("user@example.com");
    assertThat(provider.accessExpirationSeconds()).isEqualTo(60);
  }

  @Test
  void rejectsWrongTypeAndMalformedToken() {
    var auth =
        UsernamePasswordAuthenticationToken.authenticated("u@e.com", null, java.util.List.of());
    assertThatThrownBy(() -> provider.subject(provider.refreshToken(auth), "ACCESS"))
        .isInstanceOf(io.jsonwebtoken.JwtException.class);
    assertThat(provider.validAccess("bad")).isFalse();
  }

  @Test
  void rejectsShortSecret() {
    var p = new JwtTokenProvider("short", 1, 1);
    assertThatThrownBy(p::init).isInstanceOf(IllegalStateException.class);
  }
}
