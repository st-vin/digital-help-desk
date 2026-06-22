package com.mku.helpdesk.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Sent as a plain string ("OPEN", "IN_PROGRESS", "RESOLVED") rather than
 * accepting the Status enum directly in the controller signature — Jackson
 * CAN deserialize straight into an enum, but if the client sends a typo
 * or lowercase value, you get a 400 from Jackson with a fairly cryptic
 * message before your own validation or business logic ever runs. Taking
 * it as a String and parsing it explicitly in TicketService lets us throw
 * our own clear error message ("Unknown status: resolved") instead.
 */
@Data
public class StatusUpdateRequest {

    @NotBlank(message = "Status is required")
    private String status;
}
