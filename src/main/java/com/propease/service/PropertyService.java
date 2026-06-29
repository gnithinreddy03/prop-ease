package com.propease.service;

import com.propease.domain.Enums;
import com.propease.domain.Property;
import com.propease.domain.PropertyImage;
import com.propease.domain.User;
import com.propease.dto.PropertyDtos.AnalyticsResponse;
import com.propease.dto.PropertyDtos.AvailabilityRequest;
import com.propease.dto.PropertyDtos.PropertyRequest;
import com.propease.dto.PropertyDtos.PropertyResponse;
import com.propease.mapper.PropertyMapper;
import com.propease.repository.ContactRequestRepository;
import com.propease.repository.PropertyRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class PropertyService {
  private final PropertyRepository properties;
  private final ContactRequestRepository contacts;
  private final CurrentUserService current;
  private final PropertyMapper mapper;
  private final ImageStorageService images;

  @Transactional
  public PropertyResponse create(PropertyRequest request, List<MultipartFile> files) {
    User owner = current.get();
    Property p = mapper.toEntity(request);
    p.setOwner(owner);
    addImages(p, request.images(), files);
    return mapper.toResponse(properties.save(p));
  }

  @Transactional
  public PropertyResponse update(UUID id, PropertyRequest request, List<MultipartFile> files) {
    Property p = owned(id);
    mapper.update(request, p);
    if ((files != null && !files.isEmpty())
        || (request.images() != null && !request.images().isEmpty())) {
      p.getImages().clear();
      addImages(p, request.images(), files);
    }
    return mapper.toResponse(properties.save(p));
  }

  @Transactional
  public void delete(UUID id) {
    properties.delete(owned(id));
  }

  @Transactional
  public PropertyResponse availability(UUID id, AvailabilityRequest r) {
    Property p = owned(id);
    p.setAvailabilityStatus(r.status());
    return mapper.toResponse(p);
  }

  @Transactional
  public PropertyResponse details(UUID id) {
    properties.incrementViews(id);
    properties.flush();
    return mapper.toResponse(
        properties
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Property not found")));
  }

  @Transactional(readOnly = true)
  public Page<PropertyResponse> mine(Pageable page) {
    return properties.findByOwnerId(current.get().getId(), page).map(mapper::toResponse);
  }

  @Transactional(readOnly = true)
  public AnalyticsResponse analytics(UUID id) {
    Property p = owned(id);
    return new AnalyticsResponse(id, p.getViews(), contacts.countByPropertyId(id));
  }

  @Transactional(readOnly = true)
  public Page<PropertyResponse> search(
      String city,
      String area,
      BigDecimal min,
      BigDecimal max,
      Integer bedrooms,
      Integer bathrooms,
      Enums.AvailabilityStatus status,
      Instant after,
      Pageable page) {
    Specification<Property> spec =
        (root, q, cb) -> {
          List<Predicate> ps = new ArrayList<>();
          if (city != null) ps.add(cb.equal(cb.lower(root.get("city")), city.toLowerCase()));
          if (area != null) ps.add(cb.equal(cb.lower(root.get("area")), area.toLowerCase()));
          if (min != null) ps.add(cb.greaterThanOrEqualTo(root.get("rentAmount"), min));
          if (max != null) ps.add(cb.lessThanOrEqualTo(root.get("rentAmount"), max));
          if (bedrooms != null) ps.add(cb.equal(root.get("bedrooms"), bedrooms));
          if (bathrooms != null) ps.add(cb.equal(root.get("bathrooms"), bathrooms));
          if (status != null) ps.add(cb.equal(root.get("availabilityStatus"), status));
          if (after != null) ps.add(cb.greaterThanOrEqualTo(root.get("createdAt"), after));
          return cb.and(ps.toArray(Predicate[]::new));
        };
    return properties.findAll(spec, page).map(mapper::toResponse);
  }

  private Property owned(UUID id) {
    Property p =
        properties
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Property not found"));
    if (!p.getOwner().getId().equals(current.get().getId()))
      throw new AccessDeniedException("Only the property owner may perform this action");
    return p;
  }

  private void addImages(Property p, List<String> urls, List<MultipartFile> files) {
    if (urls != null)
      urls.forEach(
          url -> p.getImages().add(PropertyImage.builder().property(p).imageUrl(url).build()));
    if (files != null)
      files.stream()
          .filter(f -> !f.isEmpty())
          .map(images::store)
          .forEach(
              url -> p.getImages().add(PropertyImage.builder().property(p).imageUrl(url).build()));
  }
}
