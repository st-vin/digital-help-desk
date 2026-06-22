package com.mku.helpdesk.repository;

import com.mku.helpdesk.model.Priority;
import com.mku.helpdesk.model.Status;
import com.mku.helpdesk.model.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    // Student dashboard: "give me my own tickets, newest first."
    // Note: this is a derived query, not @Query — Spring Data JPA reads
    // the method name and writes "SELECT * FROM tickets WHERE student_id = ?
    // ORDER BY created_at DESC" for you. Free SQL, as long as the name is
    // spelled exactly right against the Ticket entity's field names
    // (student.id, not studentId — because student is a relationship).
    List<Ticket> findByStudentIdOrderByCreatedAtDesc(Long studentId);

    // Staff dashboard, unfiltered view.
    List<Ticket> findAllByOrderByPriorityDescCreatedAtAsc();

    // Staff dashboard with filters. Using @Query here instead of stacking
    // more derived-query method names, because "findByStatusAndPriorityAnd
    // CategoryIdAndCreatedAtBetween" becomes unreadable fast. Each filter
    // parameter is nullable — pass null for any filter the staff member
    // hasn't selected, and the corresponding condition is skipped.
    @Query("""
        SELECT t FROM Ticket t
        WHERE (:status IS NULL OR t.status = :status)
          AND (:priority IS NULL OR t.priority = :priority)
          AND (:categoryId IS NULL OR t.category.id = :categoryId)
        ORDER BY t.priority DESC, t.createdAt ASC
        """)
    List<Ticket> findWithFilters(@Param("status") Status status,
                                  @Param("priority") Priority priority,
                                  @Param("categoryId") Integer categoryId);

    // Used by AdminService for the per-category breakdown chart.
    long countByCategoryId(Integer categoryId);

    // Used by AdminService for the per-staff workload table.
    long countByAssignedToId(Long staffId);
    long countByAssignedToIdAndStatus(Long staffId, Status status);

    // Used by AdminService for the summary cards (total open/in-progress/
    // resolved/closed counts on the analytics dashboard).
    long countByStatus(Status status);

    // Used by AdminService to compute average resolution time. We only
    // want tickets that HAVE been resolved (resolvedAt populated) —
    // anything still OPEN or IN_PROGRESS has a null resolvedAt and would
    // throw a NullPointerException if included in a Duration.between()
    // calculation, which is why this query filters them out at the SQL
    // level instead of filtering in Java after the fact.
    List<Ticket> findByResolvedAtIsNotNull();
}
