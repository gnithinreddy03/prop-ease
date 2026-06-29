package com.propease.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "properties")
public class Property extends AuditableEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "owner_id")
  private User owner;

  @Column(nullable = false, length = 180)
  private String title;

  @Column(nullable = false, columnDefinition = "text")
  private String description;

  @Column(nullable = false)
  private String address;

  @Column(nullable = false, length = 100)
  private String city;

  @Column(nullable = false, length = 100)
  private String area;

  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal rentAmount;

  @Column(nullable = false)
  private Integer bedrooms;

  @Column(nullable = false)
  private Integer bathrooms;

  @Builder.Default
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Enums.AvailabilityStatus availabilityStatus = Enums.AvailabilityStatus.AVAILABLE;

  @Column(nullable = false)
  private String contactEmail;

  @Column(nullable = false, length = 30)
  private String contactPhone;

  @Builder.Default
  @Column(nullable = false)
  private long views = 0;

  @Builder.Default
  @OneToMany(mappedBy = "property", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<PropertyImage> images = new ArrayList<>();
}
