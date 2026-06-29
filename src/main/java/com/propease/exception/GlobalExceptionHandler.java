package com.propease.exception;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ValidationException;
import java.time.Instant;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
  public record ErrorResponse(Instant timestamp, int status, String message) {}

  @ExceptionHandler(EntityNotFoundException.class)
  ResponseEntity<ErrorResponse> notFound(Exception e) {
    return response(HttpStatus.NOT_FOUND, e.getMessage());
  }

  @ExceptionHandler(AccessDeniedException.class)
  ResponseEntity<ErrorResponse> denied(Exception e) {
    return response(HttpStatus.FORBIDDEN, e.getMessage());
  }

  @ExceptionHandler(AuthenticationException.class)
  ResponseEntity<ErrorResponse> auth(Exception e) {
    return response(HttpStatus.UNAUTHORIZED, "Authentication failed");
  }

  @ExceptionHandler({
    BusinessException.class,
    ValidationException.class,
    IllegalArgumentException.class
  })
  ResponseEntity<ErrorResponse> bad(Exception e) {
    return response(HttpStatus.BAD_REQUEST, e.getMessage());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<ErrorResponse> invalid(MethodArgumentNotValidException e) {
    String msg =
        e.getBindingResult().getFieldErrors().stream()
            .map(x -> x.getField() + ": " + x.getDefaultMessage())
            .collect(Collectors.joining(", "));
    return response(HttpStatus.BAD_REQUEST, msg);
  }

  @ExceptionHandler(BindException.class)
  ResponseEntity<ErrorResponse> bind(BindException e) {
    String msg =
        e.getBindingResult().getFieldErrors().stream()
            .map(x -> x.getField() + ": " + x.getDefaultMessage())
            .collect(Collectors.joining(", "));
    return response(HttpStatus.BAD_REQUEST, msg.isBlank() ? "Invalid request parameters" : msg);
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  ResponseEntity<ErrorResponse> mismatch(MethodArgumentTypeMismatchException e) {
    String value = e.getValue() == null ? "" : e.getValue().toString();
    return response(
        HttpStatus.BAD_REQUEST,
        "Invalid value '%s' for parameter '%s'".formatted(value, e.getName()));
  }

  @ExceptionHandler(ConstraintViolationException.class)
  ResponseEntity<ErrorResponse> constraint(ConstraintViolationException e) {
    String msg =
        e.getConstraintViolations().stream()
            .map(v -> v.getPropertyPath() + ": " + v.getMessage())
            .collect(Collectors.joining(", "));
    return response(HttpStatus.BAD_REQUEST, msg);
  }

  @ExceptionHandler(PropertyReferenceException.class)
  ResponseEntity<ErrorResponse> property(PropertyReferenceException e) {
    return response(HttpStatus.BAD_REQUEST, "Invalid sort property: " + e.getPropertyName());
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<ErrorResponse> unexpected(Exception e) {
    log.error("Unexpected application error", e);
    return response(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
  }

  private ResponseEntity<ErrorResponse> response(HttpStatus s, String m) {
    return ResponseEntity.status(s).body(new ErrorResponse(Instant.now(), s.value(), m));
  }
}
