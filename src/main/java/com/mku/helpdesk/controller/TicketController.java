package com.mku.helpdesk.controller;

import com.mku.helpdesk.dto.CategoryResponse;
import com.mku.helpdesk.dto.TicketRequest;
import com.mku.helpdesk.dto.TicketResponse;
import com.mku.helpdesk.model.User;
import com.mku.helpdesk.repository.CategoryRepository;
import com.mku.helpdesk.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;
    private final CategoryRepository categoryRepository;

    // Lives under /tickets/** so it's covered by the existing STUDENT-only
    // matcher in SecurityConfig without needing a new permitAll rule — a
    // student has to be logged in to reach the submit-ticket form anyway,
    // so requiring auth here costs nothing.
    @GetMapping("/categories")
    public ResponseEntity<List<CategoryResponse>> getCategories() {
        List<CategoryResponse> categories = categoryRepository.findAll().stream()
                .map(c -> new CategoryResponse(c.getId(), c.getName()))
                .toList();
        return ResponseEntity.ok(categories);
    }

    @PostMapping
    public ResponseEntity<TicketResponse> submit(@Valid @RequestBody TicketRequest request,
                                                  @AuthenticationPrincipal User student) {
        TicketResponse response = ticketService.submitTicket(request, student);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<TicketResponse>> myTickets(@AuthenticationPrincipal User student) {
        return ResponseEntity.ok(ticketService.getMyTickets(student.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TicketResponse> detail(@PathVariable Long id,
                                                  @AuthenticationPrincipal User student) {
        return ResponseEntity.ok(ticketService.getMyTicketDetail(id, student.getId()));
    }

    @PutMapping("/{id}/close")
    public ResponseEntity<TicketResponse> close(@PathVariable Long id,
                                                 @AuthenticationPrincipal User student) {
        return ResponseEntity.ok(ticketService.closeTicket(id, student));
    }
}
