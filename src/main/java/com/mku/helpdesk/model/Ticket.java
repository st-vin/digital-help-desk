package com.mku.helpdesk.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * The core entity. Two @ManyToOne references to User (student who filed it,
 * and the staff member it's assigned to) plus one to Category.
 *
 * IMPORTANT - fetch = FetchType.LAZY on every relationship below:
 * this means ticket.getStudent() returns a proxy, not the real User, until
 * you actually call a getter on it (ticket.getStudent().getEmail()). That
 * call only works while the original Hibernate session/transaction is still
 * open. Translate entity -> DTO INSIDE your @Transactional service method,
 * never in the controller after the service call has already returned —
 * otherwise you'll hit LazyInitializationException the first time you try
 * to read ticket.getStudent().getFullName() in a controller. This is the
 * single most common runtime crash you'll see in this project; the fix is
 * always "move the .get... call earlier, into the service layer."
 */
@Entity
@Table(name = "tickets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    // Nullable: a brand new ticket has nobody assigned yet.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to")
    private User assignedTo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Priority priority = Priority.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.OPEN;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    // Null until status moves to RESOLVED. Used for SLA / analytics reporting.
    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    // Again: no @OneToMany List<Comment> or List<TicketHistory> here.
    // Query them via CommentRepository.findByTicketId(ticketId) and
    // TicketHistoryRepository.findByTicketId(ticketId) instead.
}
