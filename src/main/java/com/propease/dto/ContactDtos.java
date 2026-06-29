package com.propease.dto;

import com.propease.domain.Enums;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public final class ContactDtos {
  private ContactDtos() {}

  public record CreateRequest(@NotNull UUID propertyId) {}

  public record Response(
      UUID id,
      UUID propertyId,
      String propertyTitle,
      UUID tenantId,
      String tenantName,
      String tenantEmail,
      Enums.ContactRequestStatus status,
      Instant createdAt) {}
}
