package com.mku.helpdesk.event;

public record TicketStatusChangedEvent(
        Long ticketId,
        String ticketTitle,
        String studentEmail,
        String studentName,
        String oldStatus,
        String newStatus,
        String changedByName
) {
}
