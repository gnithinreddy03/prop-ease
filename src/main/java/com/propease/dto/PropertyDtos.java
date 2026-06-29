package com.propease.dto;

import com.propease.domain.Enums;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

public final class PropertyDtos {
  private PropertyDtos() {}

  public record PropertyRequest(
      @NotBlank @Size(max = 180) String title,
      @NotBlank String description,
      @NotBlank String address,
      @NotBlank String city,
      @NotBlank String area,
      @NotNull @Positive BigDecimal rentAmount,
      @NotNull @PositiveOrZero Integer bedrooms,
      @NotNull @PositiveOrZero Integer bathrooms,
      @NotBlank @Email String contactEmail,
      @NotBlank @Pattern(regexp = "^[+0-9 ()-]{7,30}$") String contactPhone,
      List<@NotBlank @Size(max = 2048) String> images) {}

  public record PropertyForm(
      @NotBlank @Size(max = 180) String title,
      @NotBlank String description,
      @NotBlank String address,
      @NotBlank String city,
      @NotBlank String area,
      @NotNull @Positive BigDecimal rentAmount,
      @NotNull @PositiveOrZero Integer bedrooms,
      @NotNull @PositiveOrZero Integer bathrooms,
      @NotBlank @Email String contactEmail,
      @NotBlank @Pattern(regexp = "^[+0-9 ()-]{7,30}$") String contactPhone,
      List<MultipartFile> images) {
    public PropertyRequest request() {
      return new PropertyRequest(
          title,
          description,
          address,
          city,
          area,
          rentAmount,
          bedrooms,
          bathrooms,
          contactEmail,
          contactPhone,
          List.of());
    }
  }

  public record PropertyResponse(
      UUID id,
      UUID ownerId,
      String ownerName,
      String title,
      String description,
      String address,
      String city,
      String area,
      BigDecimal rentAmount,
      Integer bedrooms,
      Integer bathrooms,
      Enums.AvailabilityStatus availabilityStatus,
      String contactEmail,
      String contactPhone,
      long views,
      List<String> images,
      Instant createdAt,
      Instant updatedAt) {}

  public record AvailabilityRequest(@NotNull Enums.AvailabilityStatus status) {}

  public record AnalyticsResponse(UUID propertyId, long views, long contactRequests) {}
}
