package com.mku.helpdesk.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * NOTE ON LOMBOK: we use @Getter/@Setter here, NOT @Data.
 * @Data also generates equals(), hashCode() and toString() — harmless on
 * User right now since it has no relationship fields, but the moment any
 * entity in this codebase has a @ManyToOne/@OneToMany, @Data's generated
 * toString() will try to print the related entity too, which prints ITS
 * related entity, and so on -> StackOverflowError at the most awkward
 * possible moment (usually a stray log.info(user) call). Getter/Setter
 * only is the safe default for every entity in this project.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Nullable on purpose: ICT_STAFF and ADMIN accounts have no student number.
    @Column(name = "student_number", unique = true, length = 20)
    private String studentNumber;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    // Always a BCrypt hash, never plaintext. See AuthService when we build it —
    // passwordEncoder.encode() happens there, not here.
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    // EnumType.STRING is mandatory here — see the comment on Role.java.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role = Role.STUDENT;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // Deliberately NO @OneToMany List<Ticket> tickets field here.
    // We always query tickets via TicketRepository.findByStudentId(userId)
    // instead of navigating user.getTickets() — this keeps the relationship
    // one-directional and avoids the User <-> Ticket <-> User JSON
    // serialization loop entirely. Don't add it back without a real reason.
}
