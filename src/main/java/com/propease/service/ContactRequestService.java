package com.propease.service;

import com.propease.domain.ContactRequest;
import com.propease.domain.Enums;
import com.propease.domain.Property;
import com.propease.domain.User;
import com.propease.dto.ContactDtos.CreateRequest;
import com.propease.dto.ContactDtos.Response;
import com.propease.exception.BusinessException;
import com.propease.mapper.ContactRequestMapper;
import com.propease.repository.ContactRequestRepository;
import com.propease.repository.PropertyRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ContactRequestService {
  private final ContactRequestRepository contacts;
  private final PropertyRepository properties;
  private final CurrentUserService current;
  private final ContactRequestMapper mapper;
  private final EmailService email;

  @Transactional
  public Response create(CreateRequest dto) {
    User tenant = current.get();
    Property p =
        properties
            .findById(dto.propertyId())
            .orElseThrow(() -> new EntityNotFoundException("Property not found"));
    if (p.getOwner().getId().equals(tenant.getId()))
      throw new BusinessException("Owners cannot request their own contact information");
    if (contacts.existsByTenantIdAndPropertyId(tenant.getId(), p.getId()))
      throw new BusinessException("A contact request already exists");
    return mapper.toResponse(
        contacts.save(ContactRequest.builder().tenant(tenant).property(p).build()));
  }

  @Transactional(readOnly = true)
  public Page<Response> ownerRequests(Pageable page) {
    return contacts.findByPropertyOwnerId(current.get().getId(), page).map(mapper::toResponse);
  }

  @Transactional
  public Response approve(UUID id) {
    ContactRequest r = owned(id);
    if (r.getStatus() != Enums.ContactRequestStatus.PENDING)
      throw new BusinessException("Only pending requests can be approved");
    r.setStatus(Enums.ContactRequestStatus.APPROVED);
    contacts.save(r);
    email.sendApprovedContact(r);
    return mapper.toResponse(r);
  }

  @Transactional
  public Response reject(UUID id) {
    ContactRequest r = owned(id);
    if (r.getStatus() != Enums.ContactRequestStatus.PENDING)
      throw new BusinessException("Only pending requests can be rejected");
    r.setStatus(Enums.ContactRequestStatus.REJECTED);
    return mapper.toResponse(r);
  }

  private ContactRequest owned(UUID id) {
    ContactRequest r =
        contacts
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Contact request not found"));
    if (!r.getProperty().getOwner().getId().equals(current.get().getId()))
      throw new AccessDeniedException("Only the property owner may manage this request");
    return r;
  }
}
