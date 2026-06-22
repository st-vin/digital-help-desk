package com.mku.helpdesk.model;

/**
 * The three roles in the system. Stored as STRING in the database
 * (see @Enumerated(EnumType.STRING) on User.role) — never ORDINAL,
 * because ordinal storage breaks silently if this enum's order
 * ever changes.
 */
public enum Role {
    STUDENT,
    ICT_STAFF,
    ADMIN
}
