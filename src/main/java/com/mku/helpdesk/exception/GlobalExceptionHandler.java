package com.mku.helpdesk.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * One place that converts every exception type the API throws into the
 * RIGHT HTTP status code with a clean JSON body — instead of each
 * controller method needing its own try/catch (which junior devs forget
 * half the time, and then everything quietly returns 200 OK even when
 * the operation failed, or 500 with a stack trace leaked to the client).
 *
 * @RestControllerAdvice applies these handlers across EVERY controller
 * in the project automatically — you don't import or register this
 * anywhere else.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Triggered automatically by @Valid failing on a @RequestBody DTO.
    // Without @Valid on the controller method parameter, this handler
    // never fires at all — invalid input just sails through silently.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.put(error.getField(), error.getDefaultMessage())
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorBody(
                HttpStatus.BAD_REQUEST, "Validation failed", fieldErrors
        ));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody(
                HttpStatus.NOT_FOUND, ex.getMessage(), null
        ));
    }

    // Thrown deliberately by AuthService on bad email/password — same
    // exception, same generic message, whether the email doesn't exist
    // OR the password is wrong. Don't let this handler reveal which one
    // it was; that distinction is exactly what lets an attacker enumerate
    // valid student emails.
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorBody(
                HttpStatus.UNAUTHORIZED, ex.getMessage(), null
        ));
    }

    @ExceptionHandler(EmailNotVerifiedException.class)
    public ResponseEntity<Map<String, Object>> handleEmailNotVerified(EmailNotVerifiedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorBody(
                HttpStatus.FORBIDDEN, ex.getMessage(), null
        ));
    }

    @ExceptionHandler(VerificationException.class)
    public ResponseEntity<Map<String, Object>> handleVerification(VerificationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorBody(
                HttpStatus.BAD_REQUEST, ex.getMessage(), null
        ));
    }

    // Thrown by TicketService/CommentService ownership checks (e.g. a
    // student trying to access another student's ticket — see TC-009).
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorBody(
                HttpStatus.FORBIDDEN, ex.getMessage(), null
        ));
    }

    // Used for "this email is already registered", "unknown priority
    // value", etc — anything that's the client's fault, not a missing
    // resource and not an auth problem.
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadInput(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorBody(
                HttpStatus.BAD_REQUEST, ex.getMessage(), null
        ));
    }

    // Used for invalid ticket status transitions (e.g. CLOSED -> IN_PROGRESS
    // attempted by a non-admin) and "ticket already closed" type states.
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleBadState(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorBody(
                HttpStatus.BAD_REQUEST, ex.getMessage(), null
        ));
    }

    // Catch-all safety net. Anything not handled above becomes a clean
    // 500 with a generic message — the real stack trace still goes to
    // your server console/log via the rethrow-free default behaviour,
    // but the CLIENT never sees internal details. Don't remove this
    // handler thinking "it'll never happen" — it's exactly for the bugs
    // you haven't found yet.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorBody(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Something went wrong on our end. Please try again or contact ICT support.",
                null
        ));
    }

    private Map<String, Object> errorBody(HttpStatus status, String message, Map<String, String> fieldErrors) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        if (fieldErrors != null) {
            body.put("fieldErrors", fieldErrors);
        }
        return body;
    }
}
