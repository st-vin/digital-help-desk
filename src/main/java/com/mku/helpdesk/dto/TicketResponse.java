package com.mku.helpdesk.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * What the server returns for a ticket — to students, staff, or admin.
 *
 * This is built by TicketService, INSIDE the @Transactional method,
 * by reading values off the Ticket entity (including the lazy
 * student/category/assignedTo relationships) while the Hibernate
 * session is still open. Build this in the service layer, never by
 * passing the raw entity to the controller and converting it there —
 * that's exactly the pattern that triggers LazyInitializationException.
 *
 * Flat fields only (studentName, not a nested User object) — this also
 * sidesteps the User<->Ticket recursion problem entirely, since we
 * never serialize a User object at all here.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketResponse {
    private Long id;
    private String title;
    private String description;
    private String categoryName;
    private String priority;
    private String status;
    private String studentName;       // flattened from ticket.getStudent().getFullName()
    private String assignedToName;    // flattened from ticket.getAssignedTo() - null if unassigned
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime resolvedAt;
}
