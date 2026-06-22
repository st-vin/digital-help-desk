package com.mku.helpdesk.repository;

import com.mku.helpdesk.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // Used by AuthService.login() — Spring Data JPA builds this query
    // automatically from the method name. Get the casing exactly right:
    // findByEmail matches the `email` field on User. findByemail or
    // findByEMail would NOT match and would fail loudly at startup —
    // which is exactly what you want, rather than a silent runtime bug.
    Optional<User> findByEmail(String email);

    // Used by AuthService.register() to reject duplicate sign-ups
    // before hitting the database's unique constraint (nicer error
    // message than letting a SQL exception bubble up to the student).
    boolean existsByEmail(String email);

    boolean existsByStudentNumber(String studentNumber);
}
