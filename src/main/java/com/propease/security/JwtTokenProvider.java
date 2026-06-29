package com.propease.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {
  private final String secret;
  private final long accessMs;
  private final long refreshMs;
  private SecretKey key;

  public JwtTokenProvider(
      @Value("${app.jwt.secret}") String secret,
      @Value("${app.jwt.access-expiration-ms:900000}") long accessMs,
      @Value("${app.jwt.refresh-expiration-ms:604800000}") long refreshMs) {
    this.secret = secret;
    this.accessMs = accessMs;
    this.refreshMs = refreshMs;
  }

  @PostConstruct
  void init() {
    if (secret.length() < 32)
      throw new IllegalStateException("JWT_SECRET must contain at least 32 characters");
    key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
  }

  public String accessToken(Authentication a) {
    return token(a.getName(), "ACCESS", accessMs);
  }

  public String refreshToken(Authentication a) {
    return token(a.getName(), "REFRESH", refreshMs);
  }

  private String token(String subject, String type, long ttl) {
    Instant now = Instant.now();
    return Jwts.builder()
        .subject(subject)
        .claim("type", type)
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plusMillis(ttl)))
        .signWith(key)
        .compact();
  }

  public String subject(String token, String expectedType) {
    Claims c = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    if (!expectedType.equals(c.get("type", String.class)))
      throw new JwtException("Invalid token type");
    return c.getSubject();
  }

  public boolean validAccess(String token) {
    try {
      subject(token, "ACCESS");
      return true;
    } catch (JwtException | IllegalArgumentException e) {
      return false;
    }
  }

  public long accessExpirationSeconds() {
    return accessMs / 1000;
  }
}
