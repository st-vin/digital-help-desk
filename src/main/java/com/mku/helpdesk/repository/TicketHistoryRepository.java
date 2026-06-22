package com.mku.helpdesk.repository;

import com.mku.helpdesk.model.TicketHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketHistoryRepository extends JpaRepository<TicketHistory, Long> {

    // Newest first — most recent action shown at the top of the audit log.
    List<TicketHistory> findByTicketIdOrderByChangedAtDesc(Long ticketId);
}
