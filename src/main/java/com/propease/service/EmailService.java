package com.propease.service;

import com.propease.domain.ContactRequest;

public interface EmailService {
  void sendApprovedContact(ContactRequest request);
}
