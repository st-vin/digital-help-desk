package com.mku.helpdesk.controller;

import com.mku.helpdesk.dto.StatusUpdateRequest;
import com.mku.helpdesk.dto.TicketResponse;
import com.mku.helpdesk.model.Priority;
import com.mku.helpdesk.model.Status;
import com.mku.helpdesk.model.User;
import com.mku.helpdesk.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Separated from TicketController on purpose, even though the original
 * architecture doc lumped student and staff ticket endpoints into one
 * "TicketController" conceptually. In practice splitting by URL prefix
 * and role keeps each file focused on one audience and matches the
 * /api/v1/staff/** vs /api/v1/tickets/** distinction SecurityConfig
 * already enforces — a judgment call, not a deviation from the plan.
 */
@RestController
@RequestMapping("/api/v1/staff/tickets")
@RequiredArgsConstructor
public class StaffTicketController {

    private final TicketService ticketService;

    // Spring automatically converts the query string values into the
    // Status/Priority enums for you (e.g. ?status=OPEN&priority=HIGH).
    // One gotcha worth knowing: the value must match the enum constant
    // EXACTLY, including case — ?status=open (lowercase) fails Spring's
    // own conversion BEFORE this method body even runs, and you get a
    // 400 automatically. That's a feature, not a bug, but it can be
    // confusing the first time you see it in Postman.
    @GetMapping
    public ResponseEntity<List<TicketResponse>> all(
            @RequestParam(required = false) Status status,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) Integer categoryId) {
        return ResponseEntity.ok(ticketService.getAllTickets(status, priority, categoryId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TicketResponse> detail(@PathVariable Long id) {
        return ResponseEntity.ok(ticketService.getTicketDetailForStaff(id));
    }

    @PutMapping("/{id}/assign")
    public ResponseEntity<TicketResponse> assignToSelf(@PathVariable Long id,
                                                         @AuthenticationPrincipal User staff) {
        return ResponseEntity.ok(ticketService.assignToSelf(id, staff));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<TicketResponse> updateStatus(@PathVariable Long id,
                                                         @Valid @RequestBody StatusUpdateRequest request,
                                                         @AuthenticationPrincipal User staff) {
        return ResponseEntity.ok(ticketService.updateStatus(id, request.getStatus(), staff));
    }
}
