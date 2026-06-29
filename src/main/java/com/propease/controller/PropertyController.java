package com.propease.controller;

import com.propease.domain.Enums;
import com.propease.dto.PropertyDtos.AnalyticsResponse;
import com.propease.dto.PropertyDtos.AvailabilityRequest;
import com.propease.dto.PropertyDtos.PropertyRequest;
import com.propease.dto.PropertyDtos.PropertyResponse;
import com.propease.service.PropertyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/properties")
@RequiredArgsConstructor
@Tag(name = "Properties")
@Validated
public class PropertyController {
  private final PropertyService service;

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasRole('OWNER')")
  @Operation(
      summary = "Create a rental property",
      description =
          "Creates a new property listing for the authenticated OWNER. The backend automatically sets ownerId from the JWT token; "
              + "do not provide ownerId manually. Images are optional multipart files and are stored as URLs, not database binary data.")
  @ApiResponses({
    @ApiResponse(responseCode = "201", description = "Property created"),
    @ApiResponse(responseCode = "400", description = "Invalid property fields or file upload"),
    @ApiResponse(responseCode = "401", description = "Missing or invalid JWT access token"),
    @ApiResponse(responseCode = "403", description = "Authenticated user does not have OWNER role")
  })
  ResponseEntity<PropertyResponse> create(
      @Parameter(description = "Short listing title", example = "Lake View 2BHK Apartment")
          @RequestParam
          @NotBlank
          @Size(max = 180)
          String title,
      @Parameter(
              description = "Detailed property description",
              example = "Semi-furnished flat with balcony and parking")
          @RequestParam
          @NotBlank
          String description,
      @Parameter(description = "Street address or building address", example = "Plot 12, Lake Road")
          @RequestParam
          @NotBlank
          String address,
      @Parameter(description = "City where the property is located", example = "Hyderabad")
          @RequestParam
          @NotBlank
          String city,
      @Parameter(description = "Area/locality inside the city", example = "Kondapur")
          @RequestParam
          @NotBlank
          String area,
      @Parameter(description = "Monthly rent amount. Must be positive.", example = "26000")
          @RequestParam
          @NotNull
          @Positive
          BigDecimal rentAmount,
      @Parameter(description = "Number of bedrooms", example = "2")
          @RequestParam
          @NotNull
          @PositiveOrZero
          Integer bedrooms,
      @Parameter(description = "Number of bathrooms", example = "2")
          @RequestParam
          @NotNull
          @PositiveOrZero
          Integer bathrooms,
      @Parameter(
              description = "Email tenants can use after contact approval",
              example = "owner@example.com")
          @RequestParam
          @NotBlank
          @Email
          String contactEmail,
      @Parameter(
              description = "Phone number tenants can use after contact approval",
              example = "9876543210")
          @RequestParam
          @NotBlank
          @Pattern(regexp = "^[+0-9 ()-]{7,30}$")
          String contactPhone,
      @Parameter(
              description =
                  "Optional property image files. Leave empty if no images are available.")
          @RequestPart(required = false)
          List<MultipartFile> images) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            service.create(
                toRequest(
                    title,
                    description,
                    address,
                    city,
                    area,
                    rentAmount,
                    bedrooms,
                    bathrooms,
                    contactEmail,
                    contactPhone),
                images));
  }

  @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasRole('OWNER')")
  @Operation(
      summary = "Update a property",
      description =
          "Updates an existing property. Only the OWNER who created the property can update it. "
              + "If image files are provided, existing images are replaced.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Property updated"),
    @ApiResponse(responseCode = "400", description = "Invalid property fields or file upload"),
    @ApiResponse(responseCode = "401", description = "Missing or invalid JWT access token"),
    @ApiResponse(
        responseCode = "403",
        description = "Authenticated owner does not own this property"),
    @ApiResponse(responseCode = "404", description = "Property not found")
  })
  PropertyResponse update(
      @Parameter(description = "Property UUID", example = "7881e465-cc05-425a-8267-74a648237681")
          @PathVariable
          UUID id,
      @Parameter(description = "Updated listing title", example = "Updated Lake View 2BHK")
          @RequestParam
          @NotBlank
          @Size(max = 180)
          String title,
      @Parameter(description = "Updated property description") @RequestParam @NotBlank
          String description,
      @Parameter(description = "Updated street/building address") @RequestParam @NotBlank
          String address,
      @Parameter(description = "Updated city", example = "Hyderabad") @RequestParam @NotBlank
          String city,
      @Parameter(description = "Updated area/locality", example = "Kondapur")
          @RequestParam
          @NotBlank
          String area,
      @Parameter(description = "Updated monthly rent amount", example = "28000")
          @RequestParam
          @NotNull
          @Positive
          BigDecimal rentAmount,
      @Parameter(description = "Updated number of bedrooms", example = "2")
          @RequestParam
          @NotNull
          @PositiveOrZero
          Integer bedrooms,
      @Parameter(description = "Updated number of bathrooms", example = "2")
          @RequestParam
          @NotNull
          @PositiveOrZero
          Integer bathrooms,
      @Parameter(description = "Updated contact email", example = "owner@example.com")
          @RequestParam
          @NotBlank
          @Email
          String contactEmail,
      @Parameter(description = "Updated contact phone", example = "9876543210")
          @RequestParam
          @NotBlank
          @Pattern(regexp = "^[+0-9 ()-]{7,30}$")
          String contactPhone,
      @Parameter(description = "Optional replacement image files") @RequestPart(required = false)
          List<MultipartFile> images) {
    return service.update(
        id,
        toRequest(
            title,
            description,
            address,
            city,
            area,
            rentAmount,
            bedrooms,
            bathrooms,
            contactEmail,
            contactPhone),
        images);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("hasRole('OWNER')")
  @Operation(
      summary = "Delete a property",
      description =
          "Deletes a property listing. Only the owner who created the property can delete it.")
  @ApiResponses({
    @ApiResponse(responseCode = "204", description = "Property deleted"),
    @ApiResponse(responseCode = "401", description = "Missing or invalid JWT access token"),
    @ApiResponse(
        responseCode = "403",
        description = "Authenticated owner does not own this property"),
    @ApiResponse(responseCode = "404", description = "Property not found")
  })
  void delete(
      @Parameter(description = "Property UUID", example = "7881e465-cc05-425a-8267-74a648237681")
          @PathVariable
          UUID id) {
    service.delete(id);
  }

  @GetMapping("/my")
  @PreAuthorize("hasRole('OWNER')")
  @Operation(
      summary = "List my properties",
      description =
          "Returns a paginated list of properties owned by the authenticated OWNER. "
              + "Useful for owner dashboards and managing listings.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Owner properties returned"),
    @ApiResponse(responseCode = "401", description = "Missing or invalid JWT access token"),
    @ApiResponse(responseCode = "403", description = "Authenticated user does not have OWNER role")
  })
  Page<PropertyResponse> mine(
      @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable page) {
    return service.mine(page);
  }

  @GetMapping("/{id}")
  @Operation(
      summary = "Get property details",
      description =
          "Returns complete details for one property and increments the property view count. "
              + "Authenticated users with any valid role can view details.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Property details returned and view count incremented"),
    @ApiResponse(responseCode = "401", description = "Missing or invalid JWT access token"),
    @ApiResponse(responseCode = "404", description = "Property not found")
  })
  PropertyResponse details(
      @Parameter(description = "Property UUID", example = "7881e465-cc05-425a-8267-74a648237681")
          @PathVariable
          UUID id) {
    return service.details(id);
  }

  @GetMapping("/search")
  @PreAuthorize("hasAnyRole('TENANT','OWNER','GUIDE')")
  @Operation(
      summary = "Search properties",
      description =
          "Searches property listings using optional filters such as city, area, rent range, bedrooms, bathrooms, availability, and posted date. "
              + "Supports pagination and sorting. Sort must be a real property field such as rentAmount, createdAt, city, bedrooms, or bathrooms.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Matching properties returned"),
    @ApiResponse(
        responseCode = "400",
        description = "Invalid filter, date, enum, number, or sort property"),
    @ApiResponse(responseCode = "401", description = "Missing or invalid JWT access token"),
    @ApiResponse(
        responseCode = "403",
        description = "Authenticated user does not have TENANT, OWNER, or GUIDE role")
  })
  Page<PropertyResponse> search(
      @Parameter(
              description = "Filter by city, case-insensitive exact match",
              example = "Hyderabad")
          @RequestParam(required = false)
          String city,
      @Parameter(
              description = "Filter by area/locality, case-insensitive exact match",
              example = "Kondapur")
          @RequestParam(required = false)
          String area,
      @Parameter(description = "Minimum monthly rent", example = "10000")
          @RequestParam(required = false)
          BigDecimal rentMin,
      @Parameter(description = "Maximum monthly rent", example = "30000")
          @RequestParam(required = false)
          BigDecimal rentMax,
      @Parameter(description = "Exact bedroom count", example = "2") @RequestParam(required = false)
          Integer bedrooms,
      @Parameter(description = "Exact bathroom count", example = "2")
          @RequestParam(required = false)
          Integer bathrooms,
      @Parameter(
              description = "Availability filter: AVAILABLE, RENTED, or SOLD",
              example = "AVAILABLE")
          @RequestParam(required = false)
          Enums.AvailabilityStatus availability,
      @Parameter(
              description = "Only include properties posted after this timestamp",
              example = "2026-06-01T00:00:00Z")
          @RequestParam(required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          Instant postedAfter,
      @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable page) {
    return service.search(
        city, area, rentMin, rentMax, bedrooms, bathrooms, availability, postedAfter, page);
  }

  @PatchMapping("/{id}/availability")
  @PreAuthorize("hasRole('OWNER')")
  @Operation(
      summary = "Change property availability",
      description =
          "Changes a property's availability status to AVAILABLE, RENTED, or SOLD. "
              + "Only the owner who created the property can change availability.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Availability changed"),
    @ApiResponse(responseCode = "400", description = "Invalid availability status"),
    @ApiResponse(responseCode = "401", description = "Missing or invalid JWT access token"),
    @ApiResponse(
        responseCode = "403",
        description = "Authenticated owner does not own this property"),
    @ApiResponse(responseCode = "404", description = "Property not found")
  })
  PropertyResponse availability(
      @Parameter(description = "Property UUID", example = "7881e465-cc05-425a-8267-74a648237681")
          @PathVariable
          UUID id,
      @Valid @RequestBody AvailabilityRequest r) {
    return service.availability(id, r);
  }

  @GetMapping("/{id}/analytics")
  @PreAuthorize("hasRole('OWNER')")
  @Operation(
      summary = "View property analytics",
      description =
          "Returns analytics for one property, including view count and contact request count. "
              + "Only the owner who created the property can access analytics.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Analytics returned"),
    @ApiResponse(responseCode = "401", description = "Missing or invalid JWT access token"),
    @ApiResponse(
        responseCode = "403",
        description = "Authenticated owner does not own this property"),
    @ApiResponse(responseCode = "404", description = "Property not found")
  })
  AnalyticsResponse analytics(
      @Parameter(description = "Property UUID", example = "7881e465-cc05-425a-8267-74a648237681")
          @PathVariable
          UUID id) {
    return service.analytics(id);
  }

  private PropertyRequest toRequest(
      String title,
      String description,
      String address,
      String city,
      String area,
      BigDecimal rentAmount,
      Integer bedrooms,
      Integer bathrooms,
      String contactEmail,
      String contactPhone) {
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
