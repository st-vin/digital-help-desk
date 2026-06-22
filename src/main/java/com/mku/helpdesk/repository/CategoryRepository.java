package com.mku.helpdesk.repository;

import com.mku.helpdesk.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Not in the original architecture doc's repository list, but needed in
 * practice: TicketService must resolve a categoryId into a real Category
 * entity when a student submits a ticket, and the frontend submit form
 * needs an endpoint to populate its category dropdown from. JpaRepository
 * already gives us findById() and findAll() for free — no custom methods
 * needed here.
 */
public interface CategoryRepository extends JpaRepository<Category, Integer> {
}
