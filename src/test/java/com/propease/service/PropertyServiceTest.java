package com.propease.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.propease.domain.Enums;
import com.propease.domain.Property;
import com.propease.domain.User;
import com.propease.dto.PropertyDtos.AvailabilityRequest;
import com.propease.dto.PropertyDtos.PropertyResponse;
import com.propease.mapper.PropertyMapper;
import com.propease.repository.ContactRequestRepository;
import com.propease.repository.PropertyRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.access.AccessDeniedException;

class PropertyServiceTest {
  @Mock PropertyRepository properties;
  @Mock ContactRequestRepository contacts;
  @Mock CurrentUserService current;
  @Mock PropertyMapper mapper;
  @Mock ImageStorageService images;
  @InjectMocks PropertyService service;

  @BeforeEach
  void init() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void ownerCanChangeAvailability() {
    UUID ownerId = UUID.randomUUID(), propertyId = UUID.randomUUID();
    User owner = User.builder().id(ownerId).build();
    Property p =
        Property.builder()
            .id(propertyId)
            .owner(owner)
            .availabilityStatus(Enums.AvailabilityStatus.AVAILABLE)
            .build();
    when(properties.findById(propertyId)).thenReturn(Optional.of(p));
    when(current.get()).thenReturn(owner);
    when(mapper.toResponse(p)).thenAnswer(i -> response(p));
    PropertyResponse result =
        service.availability(propertyId, new AvailabilityRequest(Enums.AvailabilityStatus.RENTED));
    assertThat(result.availabilityStatus()).isEqualTo(Enums.AvailabilityStatus.RENTED);
  }

  @Test
  void nonOwnerCannotDelete() {
    UUID propertyId = UUID.randomUUID();
    Property p =
        Property.builder()
            .id(propertyId)
            .owner(User.builder().id(UUID.randomUUID()).build())
            .build();
    when(properties.findById(propertyId)).thenReturn(Optional.of(p));
    when(current.get()).thenReturn(User.builder().id(UUID.randomUUID()).build());
    assertThatThrownBy(() -> service.delete(propertyId)).isInstanceOf(AccessDeniedException.class);
    verify(properties, never()).delete(any(Property.class));
  }

  @Test
  void detailsAtomicallyIncrementsViews() {
    UUID id = UUID.randomUUID();
    Property p =
        Property.builder()
            .id(id)
            .owner(User.builder().id(UUID.randomUUID()).build())
            .views(4)
            .build();
    when(properties.findById(id)).thenReturn(Optional.of(p));
    when(mapper.toResponse(p)).thenReturn(response(p));
    service.details(id);
    verify(properties).incrementViews(id);
    verify(properties).flush();
  }

  private PropertyResponse response(Property p) {
    return new PropertyResponse(
        p.getId(),
        p.getOwner().getId(),
        null,
        null,
        null,
        null,
        null,
        null,
        BigDecimal.ONE,
        0,
        0,
        p.getAvailabilityStatus(),
        null,
        null,
        p.getViews(),
        List.of(),
        null,
        null);
  }
}
