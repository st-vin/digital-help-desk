package com.mku.helpdesk.controller;

import com.mku.helpdesk.dto.CommentRequest;
import com.mku.helpdesk.dto.CommentResponse;
import com.mku.helpdesk.model.User;
import com.mku.helpdesk.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * No class-level @RequestMapping prefix here, deliberately — the four
 * methods below sit under two different URL prefixes
 * (/api/v1/tickets/** and /api/v1/staff/tickets/**) so each one can be
 * picked up by the correct role-based matcher in SecurityConfig. Full
 * paths are spelled out on each method instead.
 */
@RestController
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    // ---- Student side ----

    @PostMapping("/api/v1/tickets/{ticketId}/comments")
    public ResponseEntity<CommentResponse> studentReply(@PathVariable Long ticketId,
                                                          @Valid @RequestBody CommentRequest request,
                                                          @AuthenticationPrincipal User student) {
        CommentResponse response = commentService.addComment(ticketId, request, student);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/api/v1/tickets/{ticketId}/comments")
    public ResponseEntity<List<CommentResponse>> studentThread(@PathVariable Long ticketId,
                                                                 @AuthenticationPrincipal User student) {
        return ResponseEntity.ok(commentService.getThread(ticketId, student));
    }

    // ---- Staff side ----

    @PostMapping("/api/v1/staff/tickets/{ticketId}/comments")
    public ResponseEntity<CommentResponse> staffReply(@PathVariable Long ticketId,
                                                        @Valid @RequestBody CommentRequest request,
                                                        @AuthenticationPrincipal User staff) {
        CommentResponse response = commentService.addComment(ticketId, request, staff);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/api/v1/staff/tickets/{ticketId}/comments")
    public ResponseEntity<List<CommentResponse>> staffThread(@PathVariable Long ticketId,
                                                                @AuthenticationPrincipal User staff) {
        return ResponseEntity.ok(commentService.getThread(ticketId, staff));
    }
}
