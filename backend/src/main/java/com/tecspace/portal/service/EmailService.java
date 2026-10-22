package com.tecspace.portal.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Sends email when SMTP is set up (spring.mail.host). If it is not, the message is printed in the
 * backend console instead, so password reset still works on your own computer.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final ObjectProvider<JavaMailSender> mailSender;

    @Value("${tecspace.mail.from:no-reply@tecspace.local}")
    private String from;

    public EmailService(ObjectProvider<JavaMailSender> mailSender) {
        this.mailSender = mailSender;
    }

    public boolean send(String to, String subject, String body) {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null) {
            log.warn("\n===== EMAIL NOT CONFIGURED, showing it here instead =====\nTo: {}\nSubject: {}\n\n{}\n=========================================================", to, subject, body);
            return false;
        }
        try {
            SimpleMailMessage m = new SimpleMailMessage();
            m.setFrom(from);
            m.setTo(to);
            m.setSubject(subject);
            m.setText(body);
            sender.send(m);
            return true;
        } catch (MailException e) {
            log.error("Could not send email to {}: {}", to, e.getMessage());
            return false;
        }
    }
}
