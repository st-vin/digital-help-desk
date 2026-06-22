package com.mku.helpdesk.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * What a student sends to POST /api/v1/tickets.
 *
 * Deliberately does NOT have an `id`, `status`, or `assignedTo` field.
 * This is the actual security boundary, not just convention: if this
 * DTO had a `status` field, a student could include
 * {"status": "RESOLVED"} in their submission JSON and mark their own
 * ticket resolved on creation. Separate request/response DTOs are how
 * you make that structurally impossible rather than relying on the
 * service layer to "remember" to ignore it.
 */
@Data
public class TicketRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must be under 200 characters")
    private String title;

    @NotNull(message = "Category is required")
    private Integer categoryId;

    @NotBlank(message = "Priority is required")
    private String priority; // "LOW" | "MEDIUM" | "HIGH" | "CRITICAL" — parsed to the Priority enum in the service layer

    @NotBlank(message = "Description is required")
    @Size(min = 20, message = "Please provide at least 20 characters describing the issue")
    private String description;
}
