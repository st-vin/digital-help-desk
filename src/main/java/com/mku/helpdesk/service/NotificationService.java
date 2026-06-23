package com.mku.helpdesk.service;

import com.mku.helpdesk.event.TicketStatusChangedEvent;
import com.mku.helpdesk.event.VerificationEmailRequestedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final JavaMailSender mailSender;

    @Value("${app.mail-from:no-reply@digital-help-desk.local}")
    private String fromAddress;

    @Value("${app.backend-base-url:http://localhost:7777}")
    private String backendBaseUrl;

    public void sendVerificationEmail(VerificationEmailRequestedEvent event) {
        String subject = "Verify your Digital Help Desk account";
        String verificationLink = backendBaseUrl + "/api/v1/auth/verify-email?token=" + event.token();
        String body = """
                Hello %s,

                Your account was created successfully.
                Verify your email address by opening this link:
                %s

                If you did not register, you can ignore this message.
                """.formatted(event.fullName(), verificationLink);

        sendMail(event.email(), subject, body);
    }

    public void sendTicketStatusEmail(TicketStatusChangedEvent event) {
        String subject = "Your ticket status changed: #" + event.ticketId();
        String body = """
                Hello %s,

                Your ticket "%s" changed status.
                Old status: %s
                New status: %s
                Updated by: %s

                You can log in to the portal to review the latest update.
                """.formatted(
                event.studentName(),
                event.ticketTitle(),
                event.oldStatus(),
                event.newStatus(),
                event.changedByName()
        );

        sendMail(event.studentEmail(), subject, body);
    }

    private void sendMail(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }
}
