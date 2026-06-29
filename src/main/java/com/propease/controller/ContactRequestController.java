package com.propease.controller;

import com.propease.dto.ContactDtos.CreateRequest;
import com.propease.dto.ContactDtos.Response;
import com.propease.service.ContactRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/contact-requests")
@RequiredArgsConstructor
@Tag(name = "Contact Requests")
public class ContactRequestController {
  private final ContactRequestService service;

  @PostMapping
  @PreAuthorize("hasRole('TENANT')")
  @Operation(
      summary = "Request owner contact details",
      description =
          "Creates a PENDING contact request for a property. Only TENANT users can request contact information. "
              + "The owner must approve the request before contact details are emailed to the tenant.")
  @ApiResponses({
    @ApiResponse(responseCode = "201", description = "Contact request created with PENDING status"),
    @ApiResponse(
        responseCode = "400",
        description = "Invalid property id or duplicate/business-rule violation"),
    @ApiResponse(responseCode = "401", description = "Missing or invalid JWT access token"),
    @ApiResponse(
        responseCode = "403",
        description = "Authenticated user does not have TENANT role"),
    @ApiResponse(responseCode = "404", description = "Property not found")
  })
  ResponseEntity<Response> create(@Valid @RequestBody CreateRequest r) {
    return ResponseEntity.status(HttpStatus.CREATED).body(service.create(r));
  }

  @GetMapping
  @PreAuthorize("hasRole('OWNER')")
  @Operation(
      summary = "List contact requests for my properties",
      description =
          "Returns paginated contact requests for properties owned by the authenticated OWNER. "
              + "Owners cannot see requests for other owners' properties.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Owner contact requests returned"),
    @ApiResponse(responseCode = "401", description = "Missing or invalid JWT access token"),
    @ApiResponse(responseCode = "403", description = "Authenticated user does not have OWNER role")
  })
  Page<Response> list(
      @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable p) {
    return service.ownerRequests(p);
  }

  @PatchMapping("/{id}/approve")
  @PreAuthorize("hasRole('OWNER')")
  @Operation(
      summary = "Approve a contact request",
      description =
          "Approves a PENDING contact request for one of the authenticated owner's properties. "
              + "After approval, the system sends an email to the tenant containing owner contact details.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Request approved and approval email triggered"),
    @ApiResponse(
        responseCode = "400",
        description = "Request is already processed or cannot be approved"),
    @ApiResponse(responseCode = "401", description = "Missing or invalid JWT access token"),
    @ApiResponse(
        responseCode = "403",
        description = "Authenticated owner does not own the requested property"),
    @ApiResponse(responseCode = "404", description = "Contact request not found")
  })
  Response approve(
      @Parameter(
              description = "Contact request UUID",
              example = "f6f7cc51-b63e-4afb-a11a-1fca699b5385")
          @PathVariable
          UUID id) {
    return service.approve(id);
  }

  @PatchMapping("/{id}/reject")
  @PreAuthorize("hasRole('OWNER')")
  @Operation(
      summary = "Reject a contact request",
      description =
          "Rejects a PENDING contact request for one of the authenticated owner's properties. "
              + "No contact email is sent when a request is rejected.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Request rejected"),
    @ApiResponse(
        responseCode = "400",
        description = "Request is already processed or cannot be rejected"),
    @ApiResponse(responseCode = "401", description = "Missing or invalid JWT access token"),
    @ApiResponse(
        responseCode = "403",
        description = "Authenticated owner does not own the requested property"),
    @ApiResponse(responseCode = "404", description = "Contact request not found")
  })
  Response reject(
      @Parameter(
              description = "Contact request UUID",
              example = "f6f7cc51-b63e-4afb-a11a-1fca699b5385")
          @PathVariable
          UUID id) {
    return service.reject(id);
  }
}
