package com.mku.helpdesk.exception;

/**
 * Thrown when a ticket, user, or category id is looked up and doesn't
 * exist. Caught by GlobalExceptionHandler and turned into a clean 404 —
 * without this, a missing entity would surface as a NullPointerException
 * somewhere downstream, returned to the client as a 500 with zero useful
 * information.
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
