package com.propease.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.propease.dto.AuthDtos.AuthResponse;
import com.propease.exception.GlobalExceptionHandler;
import com.propease.service.AuthService;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AuthControllerTest {
  MockMvc mvc;
  AuthService service;

  @org.junit.jupiter.api.BeforeEach
  void setUp() {
    service = org.mockito.Mockito.mock(AuthService.class);
    mvc =
        MockMvcBuilders.standaloneSetup(new AuthController(service))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
  }

  @Test
  void loginReturnsTokens() throws Exception {
    when(service.login(any()))
        .thenReturn(new AuthResponse("a", "r", "Bearer", 900, Set.of("OWNER")));
    mvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"john@example.com\",\"password\":\"Password1\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").value("a"));
  }

  @Test
  void registrationValidationIsConsistent() throws Exception {
    mvc.perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"fullName\":\"\",\"email\":\"bad\",\"password\":\"weak\",\"roles\":[]}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400));
  }
}
