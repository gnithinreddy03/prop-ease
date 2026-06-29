package com.propease.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Set;

public final class AuthDtos {
  private AuthDtos() {}

  public record RegisterRequest(
      @NotBlank @Size(max = 150) String fullName,
      @NotBlank @Email String email,
      @NotBlank
          @Pattern(
              regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$",
              message = "must contain uppercase, lowercase, number and at least 8 characters")
          String password,
      @NotEmpty Set<@Pattern(regexp = "OWNER|TENANT|GUIDE") String> roles) {}

  public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}

  public record RefreshRequest(@NotBlank String refreshToken) {}

  public record AuthResponse(
      String accessToken,
      String refreshToken,
      String tokenType,
      long expiresIn,
      Set<String> roles) {}
}
