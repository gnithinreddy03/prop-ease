package com.propease.repository;

import com.propease.domain.Property;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PropertyRepository
    extends JpaRepository<Property, UUID>, JpaSpecificationExecutor<Property> {
  Page<Property> findByOwnerId(UUID ownerId, Pageable pageable);

  @Modifying
  @Query("update Property p set p.views=p.views+1 where p.id=:id")
  int incrementViews(@Param("id") UUID id);
}
