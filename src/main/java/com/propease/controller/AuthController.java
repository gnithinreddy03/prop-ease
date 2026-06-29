package com.propease.controller;

import com.propease.dto.AuthDtos.AuthResponse;
import com.propease.dto.AuthDtos.LoginRequest;
import com.propease.dto.AuthDtos.RefreshRequest;
import com.propease.dto.AuthDtos.RegisterRequest;
import com.propease.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication")
public class AuthController {
  private final AuthService service;

  @PostMapping("/register")
  @Operation(
      summary = "Register a new user",
      description =
          "Creates a user account with one or more roles. Supported roles are OWNER, TENANT, and GUIDE. "
              + "Passwords are stored using BCrypt. Use this endpoint before login when creating a new test user.")
  @ApiResponses({
    @ApiResponse(responseCode = "201", description = "User registered and JWT tokens returned"),
    @ApiResponse(responseCode = "400", description = "Invalid registration data or role selection"),
    @ApiResponse(responseCode = "409", description = "Email already exists")
  })
  ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest r) {
    return ResponseEntity.status(HttpStatus.CREATED).body(service.register(r));
  }

  @PostMapping("/login")
  @Operation(
      summary = "Login and get JWT tokens",
      description =
          "Authenticates with email and password. Returns an access token for API calls and a refresh token for getting a new access token. "
              + "In Swagger Authorize, paste only the access token value; Swagger adds the Bearer prefix.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Login successful"),
    @ApiResponse(responseCode = "400", description = "Invalid request body"),
    @ApiResponse(responseCode = "401", description = "Invalid email or password")
  })
  AuthResponse login(@Valid @RequestBody LoginRequest r) {
    return service.login(r);
  }

  @PostMapping("/refresh")
  @Operation(
      summary = "Refresh an access token",
      description =
          "Exchanges a valid refresh token for a new access token and refresh token pair. "
              + "Use this when the short-lived access token expires.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "New token pair returned"),
    @ApiResponse(responseCode = "400", description = "Invalid refresh request"),
    @ApiResponse(
        responseCode = "401",
        description = "Refresh token is missing, expired, or invalid")
  })
  AuthResponse refresh(@Valid @RequestBody RefreshRequest r) {
    return service.refresh(r);
  }
}
