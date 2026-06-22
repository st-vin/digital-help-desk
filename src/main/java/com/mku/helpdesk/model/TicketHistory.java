package com.mku.helpdesk.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * Audit log. One row per meaningful change to a ticket: status change,
 * assignment change, priority escalation. This is what your
 * service layer needs to remember to write to every time it changes
 * something on a Ticket — it's easy to forget because it's a "side
 * effect" of the main action, not the main action itself.
 *
 * fieldChanged examples: "status", "assignedTo", "priority"
 * oldValue / newValue: store as plain strings (e.g. "OPEN" -> "IN_PROGRESS")
 * so this table never needs to join against anything to be human-readable.
 */
@Entity
@Table(name = "ticket_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TicketHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by", nullable = false)
    private User changedBy;

    @Column(name = "field_changed", nullable = false, length = 50)
    private String fieldChanged;

    @Column(name = "old_value", length = 100)
    private String oldValue;

    @Column(name = "new_value", length = 100)
    private String newValue;

    @Column(name = "changed_at", nullable = false, updatable = false)
    private LocalDateTime changedAt = LocalDateTime.now();
}
