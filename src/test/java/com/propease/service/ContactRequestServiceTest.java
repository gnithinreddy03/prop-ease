package com.propease.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.propease.domain.ContactRequest;
import com.propease.domain.Enums;
import com.propease.domain.Property;
import com.propease.domain.User;
import com.propease.dto.ContactDtos;
import com.propease.mapper.ContactRequestMapper;
import com.propease.repository.ContactRequestRepository;
import com.propease.repository.PropertyRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.access.AccessDeniedException;

class ContactRequestServiceTest {
  @Mock ContactRequestRepository contacts;
  @Mock PropertyRepository properties;
  @Mock CurrentUserService current;
  @Mock ContactRequestMapper mapper;
  @Mock EmailService email;
  @InjectMocks ContactRequestService service;

  @BeforeEach
  void init() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void ownerApprovesPendingRequestAndEmailIsSent() {
    UUID ownerId = UUID.randomUUID();
    User owner = User.builder().id(ownerId).build();
    Property p = Property.builder().owner(owner).build();
    ContactRequest r =
        ContactRequest.builder()
            .id(UUID.randomUUID())
            .property(p)
            .status(Enums.ContactRequestStatus.PENDING)
            .build();
    when(contacts.findById(r.getId())).thenReturn(Optional.of(r));
    when(current.get()).thenReturn(owner);
    when(mapper.toResponse(r))
        .thenReturn(
            new ContactDtos.Response(
                r.getId(),
                null,
                null,
                null,
                null,
                null,
                Enums.ContactRequestStatus.APPROVED,
                null));
    var result = service.approve(r.getId());
    assertThat(result.status()).isEqualTo(Enums.ContactRequestStatus.APPROVED);
    verify(email).sendApprovedContact(r);
  }

  @Test
  void differentOwnerCannotApprove() {
    User actual = User.builder().id(UUID.randomUUID()).build();
    Property p = Property.builder().owner(User.builder().id(UUID.randomUUID()).build()).build();
    ContactRequest r = ContactRequest.builder().id(UUID.randomUUID()).property(p).build();
    when(contacts.findById(r.getId())).thenReturn(Optional.of(r));
    when(current.get()).thenReturn(actual);
    assertThatThrownBy(() -> service.approve(r.getId())).isInstanceOf(AccessDeniedException.class);
  }
}
