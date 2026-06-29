package com.propease.repository;

import com.propease.domain.ContactRequest;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContactRequestRepository extends JpaRepository<ContactRequest, UUID> {
  @EntityGraph(attributePaths = {"tenant", "property", "property.owner"})
  Page<ContactRequest> findByPropertyOwnerId(UUID ownerId, Pageable pageable);

  long countByPropertyId(UUID propertyId);

  boolean existsByTenantIdAndPropertyId(UUID tenantId, UUID propertyId);
}
