-- ============================================================
-- schema.sql (Optimized with Explicit FK Constraints)
--
-- RUN THIS FIRST, manually, before starting the Spring Boot app.
-- Because application.properties uses ddl-auto=validate (not update/
-- create), Hibernate will NOT create these tables for you — it only
-- checks that your @Entity classes match what's already here. If you
-- start the app before running this file, you'll get a confusing
-- "Schema-validation: missing table" error. Run this first, every time
-- you set up the project on a new machine.
--
-- How to run it:
--   mysql -u root -p < sql/schema.sql
-- ============================================================

CREATE DATABASE IF NOT EXISTS helpdesk CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE helpdesk;

-- 1. Create Users Table
CREATE TABLE IF NOT EXISTS users (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_number  VARCHAR(20) UNIQUE,
    full_name       VARCHAR(100) NOT NULL,
    email           VARCHAR(150) NOT NULL UNIQUE,
    email_verified  BOOLEAN NOT NULL DEFAULT FALSE,
    verified_at     DATETIME NULL,
    password_hash   VARCHAR(255) NOT NULL,
    role            ENUM('STUDENT','ICT_STAFF','ADMIN') NOT NULL DEFAULT 'STUDENT',
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. Create Categories Table
CREATE TABLE IF NOT EXISTS categories (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255)
);

-- 3. Create Tickets Table
CREATE TABLE IF NOT EXISTS tickets (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id   BIGINT NOT NULL,
    assigned_to  BIGINT,
    category_id  INT NOT NULL,
    title        VARCHAR(200) NOT NULL,
    description  TEXT NOT NULL,
    priority     ENUM('LOW','MEDIUM','HIGH','CRITICAL') NOT NULL DEFAULT 'MEDIUM',
    status       ENUM('OPEN','IN_PROGRESS','RESOLVED','CLOSED') NOT NULL DEFAULT 'OPEN',
    created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    resolved_at  DATETIME,
    CONSTRAINT fk_tickets_student FOREIGN KEY (student_id)
        REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_tickets_assigned_to FOREIGN KEY (assigned_to)
        REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_tickets_category FOREIGN KEY (category_id)
        REFERENCES categories(id) ON DELETE RESTRICT
);

-- 4. Create Ticket Comments Table
CREATE TABLE IF NOT EXISTS ticket_comments (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_id  BIGINT NOT NULL,
    author_id  BIGINT NOT NULL,
    content    TEXT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_comments_ticket FOREIGN KEY (ticket_id)
        REFERENCES tickets(id) ON DELETE CASCADE,
    CONSTRAINT fk_comments_author FOREIGN KEY (author_id)
        REFERENCES users(id) ON DELETE RESTRICT
);

-- 5. Create Ticket History Table
CREATE TABLE IF NOT EXISTS ticket_history (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_id     BIGINT NOT NULL,
    changed_by    BIGINT NOT NULL,
    field_changed VARCHAR(50) NOT NULL,
    old_value     VARCHAR(100),
    new_value     VARCHAR(100),
    changed_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_history_ticket FOREIGN KEY (ticket_id)
        REFERENCES tickets(id) ON DELETE CASCADE,
    CONSTRAINT fk_history_changed_by FOREIGN KEY (changed_by)
        REFERENCES users(id) ON DELETE RESTRICT
);

-- 6. Create Email Verification Tokens Table
CREATE TABLE IF NOT EXISTS email_verification_tokens (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    token       VARCHAR(100) NOT NULL UNIQUE,
    user_id     BIGINT NOT NULL,
    expires_at  DATETIME NOT NULL,
    used_at     DATETIME NULL,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_email_verification_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE
);

-- Seed categories — needed before you can submit any ticket via the API.
INSERT INTO categories (name) VALUES
  ('Password reset / account locked'),
  ('Course registration failure'),
  ('Missing or incorrect exam results'),
  ('Fee statement error'),
  ('New student account activation'),
  ('Portal access / login problem'),
  ('Computer lab equipment fault'),
  ('Network / Wi-Fi connectivity'),
  ('Other')
ON DUPLICATE KEY UPDATE name = name;
