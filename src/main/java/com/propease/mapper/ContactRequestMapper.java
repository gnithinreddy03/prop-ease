package com.propease.mapper;

import com.propease.domain.ContactRequest;
import com.propease.dto.ContactDtos;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ContactRequestMapper {
  @Mapping(target = "propertyId", source = "property.id")
  @Mapping(target = "propertyTitle", source = "property.title")
  @Mapping(target = "tenantId", source = "tenant.id")
  @Mapping(target = "tenantName", source = "tenant.fullName")
  @Mapping(target = "tenantEmail", source = "tenant.email")
  ContactDtos.Response toResponse(ContactRequest request);
}
