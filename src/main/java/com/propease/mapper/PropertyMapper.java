package com.propease.mapper;

import com.propease.domain.Property;
import com.propease.domain.PropertyImage;
import com.propease.dto.PropertyDtos;
import java.util.List;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PropertyMapper {
  @Mapping(target = "ownerId", source = "owner.id")
  @Mapping(target = "ownerName", source = "owner.fullName")
  @Mapping(target = "images", source = "images", qualifiedByName = "urls")
  PropertyDtos.PropertyResponse toResponse(Property property);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "owner", ignore = true)
  @Mapping(target = "images", ignore = true)
  @Mapping(target = "availabilityStatus", ignore = true)
  @Mapping(target = "views", ignore = true)
  Property toEntity(PropertyDtos.PropertyRequest request);

  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "owner", ignore = true)
  @Mapping(target = "images", ignore = true)
  @Mapping(target = "availabilityStatus", ignore = true)
  @Mapping(target = "views", ignore = true)
  void update(PropertyDtos.PropertyRequest request, @MappingTarget Property property);

  @Named("urls")
  default List<String> urls(List<PropertyImage> images) {
    return images.stream().map(PropertyImage::getImageUrl).toList();
  }
}
