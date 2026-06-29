package com.propease.domain;

public final class Enums {
  private Enums() {}

  public enum RoleName {
    ROLE_OWNER,
    ROLE_TENANT,
    ROLE_GUIDE
  }

  public enum AvailabilityStatus {
    AVAILABLE,
    RENTED,
    SOLD
  }

  public enum ContactRequestStatus {
    PENDING,
    APPROVED,
    REJECTED
  }
}
