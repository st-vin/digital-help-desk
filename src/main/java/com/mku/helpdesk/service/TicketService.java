package com.mku.helpdesk.service;

import com.mku.helpdesk.dto.TicketRequest;
import com.mku.helpdesk.dto.TicketResponse;
import com.mku.helpdesk.exception.ResourceNotFoundException;
import com.mku.helpdesk.model.*;
import com.mku.helpdesk.repository.CategoryRepository;
import com.mku.helpdesk.repository.TicketHistoryRepository;
import com.mku.helpdesk.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    private final CategoryRepository categoryRepository;
    private final TicketHistoryRepository ticketHistoryRepository;

    // ===================== STUDENT-FACING =====================

    @Transactional
    public TicketResponse submitTicket(TicketRequest request, User student) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found."));

        Priority priority = parsePriority(request.getPriority());

        Ticket ticket = new Ticket();
        ticket.setStudent(student);
        ticket.setCategory(category);
        ticket.setTitle(request.getTitle());
        ticket.setDescription(request.getDescription());
        ticket.setPriority(priority);
        ticket.setStatus(Status.OPEN);
        ticket = ticketRepository.save(ticket);

        logHistory(ticket, student, "status", null, Status.OPEN.name());

        // toResponse() is called here, INSIDE this @Transactional method,
        // while the Hibernate session is still open — that's what lets us
        // safely call ticket.getCategory().getName() and
        // ticket.getStudent().getFullName() below despite those being
        // LAZY relationships. Do this conversion in the controller instead
        // and you'll hit LazyInitializationException the moment the
        // session closes after the service method returns.
        return toResponse(ticket);
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> getMyTickets(Long studentId) {
        return ticketRepository.findByStudentIdOrderByCreatedAtDesc(studentId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TicketResponse getMyTicketDetail(Long ticketId, Long studentId) {
        Ticket ticket = getTicketOrThrow(ticketId);
        assertOwnership(ticket, studentId);
        return toResponse(ticket);
    }

    @Transactional
    public TicketResponse closeTicket(Long ticketId, User student) {
        Ticket ticket = getTicketOrThrow(ticketId);
        assertOwnership(ticket, student.getId());

        if (ticket.getStatus() == Status.CLOSED) {
            throw new IllegalStateException("This ticket is already closed.");
        }

        String oldStatus = ticket.getStatus().name();
        ticket.setStatus(Status.CLOSED);
        ticket.setUpdatedAt(LocalDateTime.now());
        logHistory(ticket, student, "status", oldStatus, Status.CLOSED.name());

        return toResponse(ticket);
    }

    // ===================== STAFF-FACING =====================

    @Transactional(readOnly = true)
    public List<TicketResponse> getAllTickets(Status status, Priority priority, Integer categoryId) {
        return ticketRepository.findWithFilters(status, priority, categoryId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TicketResponse getTicketDetailForStaff(Long ticketId) {
        // No ownership check here on purpose — any ICT_STAFF or ADMIN can
        // view any ticket. SecurityConfig's role matcher is what's actually
        // restricting WHO can reach this method at all.
        return toResponse(getTicketOrThrow(ticketId));
    }

    @Transactional
    public TicketResponse assignToSelf(Long ticketId, User staff) {
        Ticket ticket = getTicketOrThrow(ticketId);
        String oldAssignee = ticket.getAssignedTo() != null
                ? ticket.getAssignedTo().getFullName()
                : "Unassigned";

        ticket.setAssignedTo(staff);
        ticket.setUpdatedAt(LocalDateTime.now());
        logHistory(ticket, staff, "assignedTo", oldAssignee, staff.getFullName());

        return toResponse(ticket);
    }

    @Transactional
    public TicketResponse updateStatus(Long ticketId, String newStatusRaw, User staff) {
        Ticket ticket = getTicketOrThrow(ticketId);
        Status newStatus = parseStatus(newStatusRaw);

        validateTransition(ticket.getStatus(), newStatus);

        String oldStatus = ticket.getStatus().name();
        ticket.setStatus(newStatus);
        ticket.setUpdatedAt(LocalDateTime.now());

        if (newStatus == Status.RESOLVED) {
            ticket.setResolvedAt(LocalDateTime.now());
        }

        logHistory(ticket, staff, "status", oldStatus, newStatus.name());

        // NOTE: this is exactly where NotificationService.sendStatusUpdate()
        // gets called once we build it in Phase 4 — deliberately not wired
        // up yet, so don't be alarmed that no email goes out when you test
        // this today. That's expected, not a bug.

        return toResponse(ticket);
    }

    // ===================== INTERNAL HELPERS =====================

    /**
     * Enforces the status state machine from the Technical Design Document:
     *   OPEN -> IN_PROGRESS
     *   IN_PROGRESS -> RESOLVED or back to OPEN
     *   RESOLVED -> CLOSED or back to OPEN (reopen)
     *   CLOSED -> nothing (only an admin-only reopen endpoint would bypass
     *             this, which we haven't built — out of scope for today)
     * Anything outside this table throws IllegalStateException, which
     * GlobalExceptionHandler turns into a clean 400.
     */
    private void validateTransition(Status from, Status to) {
        boolean valid = switch (from) {
            case OPEN -> to == Status.IN_PROGRESS;
            case IN_PROGRESS -> to == Status.RESOLVED || to == Status.OPEN;
            case RESOLVED -> to == Status.CLOSED || to == Status.OPEN;
            case CLOSED -> false;
        };
        if (!valid) {
            throw new IllegalStateException(
                    "Invalid status transition from " + from + " to " + to + ".");
        }
    }

    private Priority parsePriority(String raw) {
        try {
            return Priority.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    "Priority must be one of LOW, MEDIUM, HIGH, CRITICAL.");
        }
    }

    private Status parseStatus(String raw) {
        try {
            return Status.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unknown status: " + raw);
        }
    }

    private Ticket getTicketOrThrow(Long id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket #" + id + " not found."));
    }

    /**
     * THE check that stops a student from reading another student's
     * ticket by guessing IDs in the URL (e.g. GET /api/v1/tickets/47).
     * This must live here, in the service layer, NOT just be assumed from
     * the frontend hiding a button — see TC-009 in the test cases. Test
     * this exact scenario in Postman: log in as student A, try to GET
     * student B's ticket ID, confirm you get a 403.
     */
    private void assertOwnership(Ticket ticket, Long studentId) {
        if (!ticket.getStudent().getId().equals(studentId)) {
            throw new AccessDeniedException("You do not have permission to access this ticket.");
        }
    }

    private void logHistory(Ticket ticket, User changedBy, String field, String oldValue, String newValue) {
        TicketHistory history = new TicketHistory();
        history.setTicket(ticket);
        history.setChangedBy(changedBy);
        history.setFieldChanged(field);
        history.setOldValue(oldValue);
        history.setNewValue(newValue);
        ticketHistoryRepository.save(history);
    }

    private TicketResponse toResponse(Ticket t) {
        return TicketResponse.builder()
                .id(t.getId())
                .title(t.getTitle())
                .description(t.getDescription())
                .categoryName(t.getCategory().getName())
                .priority(t.getPriority().name())
                .status(t.getStatus().name())
                .studentName(t.getStudent().getFullName())
                .assignedToName(t.getAssignedTo() != null ? t.getAssignedTo().getFullName() : null)
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .resolvedAt(t.getResolvedAt())
                .build();
    }
}
