package com.mku.helpdesk.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Lookup table — seeded once via sql/schema.sql (categories like
 * "Password reset / account locked", "Fee statement error", etc).
 * No service needed to create these; admin doesn't manage them through
 * the UI in v1. If you want admins to manage categories later, that's
 * a clean v2 feature to bolt on.
 */
@Entity
@Table(name = "categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(length = 255)
    private String description;
}
