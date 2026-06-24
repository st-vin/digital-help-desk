package com.mku.helpdesk.listener;

import com.mku.helpdesk.event.TicketStatusChangedEvent;
import com.mku.helpdesk.event.VerificationEmailRequestedEvent;
import com.mku.helpdesk.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailNotificationListener {

    private final NotificationService notificationService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onVerificationEmailRequested(VerificationEmailRequestedEvent event) {
        try {
            notificationService.sendVerificationEmail(event);
        } catch (Exception ex) {
            log.error("Failed to send verification email to {}", event.email(), ex);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTicketStatusChanged(TicketStatusChangedEvent event) {
        try {
            notificationService.sendTicketStatusEmail(event);
        } catch (Exception ex) {
            log.error("Failed to send ticket status email for ticket {}", event.ticketId(), ex);
        }
    }
}
