package com.propease.service;

import com.propease.domain.ContactRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmtpEmailService implements EmailService {
  private final JavaMailSender sender;

  @Value("${spring.mail.username:no-reply@prop-ease.local}")
  private String from;

  @Async
  @Override
  public void sendApprovedContact(ContactRequest r) {
    var owner = r.getProperty().getOwner();
    SimpleMailMessage m = new SimpleMailMessage();
    m.setFrom(from);
    m.setTo(r.getTenant().getEmail());
    m.setSubject("Contact request approved: " + r.getProperty().getTitle());
    m.setText(
        "Owner Name: "
            + owner.getFullName()
            + "\nOwner Email: "
            + r.getProperty().getContactEmail()
            + "\nOwner Phone: "
            + r.getProperty().getContactPhone());
    try {
      sender.send(m);
    } catch (RuntimeException e) {
      log.error("Approval email failed for request {}", r.getId(), e);
    }
  }
}
