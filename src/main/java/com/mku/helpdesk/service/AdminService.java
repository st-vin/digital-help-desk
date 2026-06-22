package com.mku.helpdesk.service;

import com.mku.helpdesk.dto.*;
import com.mku.helpdesk.exception.ResourceNotFoundException;
import com.mku.helpdesk.model.Role;
import com.mku.helpdesk.model.Status;
import com.mku.helpdesk.model.Ticket;
import com.mku.helpdesk.model.User;
import com.mku.helpdesk.repository.CategoryRepository;
import com.mku.helpdesk.repository.TicketRepository;
import com.mku.helpdesk.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;
    private final CategoryRepository categoryRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponse createStaff(CreateStaffRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("This email address is already registered.");
        }

        User staff = new User();
        staff.setFullName(request.getFullName());
        staff.setEmail(request.getEmail());
        staff.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        staff.setRole(Role.ICT_STAFF);
        staff = userRepository.save(staff);

        return toUserResponse(staff);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> listStaff() {
        return userRepository.findAll().stream()
                .filter(u -> u.getRole() != Role.STUDENT) // admin manages staff + sees other admins, not the student list
                .map(this::toUserResponse)
                .toList();
    }

    @Transactional
    public UserResponse toggleActive(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        if (user.getRole() == Role.ADMIN) {
            throw new IllegalStateException("Admin accounts cannot be deactivated from the UI.");
        }

        user.setActive(!user.isActive());
        return toUserResponse(user);
    }

    @Transactional(readOnly = true)
    public AnalyticsResponse getAnalytics() {
        long total = ticketRepository.count();
        long open = ticketRepository.countByStatus(Status.OPEN);
        long inProgress = ticketRepository.countByStatus(Status.IN_PROGRESS);
        long resolved = ticketRepository.countByStatus(Status.RESOLVED);
        long closed = ticketRepository.countByStatus(Status.CLOSED);

        double avgHours = computeAverageResolutionHours();

        List<CategoryCountResponse> categoryCounts = categoryRepository.findAll().stream()
                .map(c -> new CategoryCountResponse(c.getName(), ticketRepository.countByCategoryId(c.getId())))
                .toList();

        List<StaffPerformanceResponse> staffPerformance = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.ICT_STAFF)
                .map(u -> new StaffPerformanceResponse(
                        u.getFullName(),
                        ticketRepository.countByAssignedToId(u.getId()),
                        ticketRepository.countByAssignedToIdAndStatus(u.getId(), Status.RESOLVED)
                ))
                .toList();

        return AnalyticsResponse.builder()
                .totalTickets(total)
                .openTickets(open)
                .inProgressTickets(inProgress)
                .resolvedTickets(resolved)
                .closedTickets(closed)
                .averageResolutionHours(avgHours)
                .ticketsByCategory(categoryCounts)
                .staffPerformance(staffPerformance)
                .build();
    }

    /**
     * Computed in Java rather than a single SQL AVG() query — deliberately
     * simple for a learning project. We only look at tickets where
     * resolvedAt is populated (findByResolvedAtIsNotNull), which is what
     * stops Duration.between() from throwing a NullPointerException on
     * tickets that are still OPEN or IN_PROGRESS.
     */
    private double computeAverageResolutionHours() {
        List<Ticket> resolvedTickets = ticketRepository.findByResolvedAtIsNotNull();
        if (resolvedTickets.isEmpty()) {
            return 0.0;
        }
        double totalMinutes = resolvedTickets.stream()
                .mapToLong(t -> Duration.between(t.getCreatedAt(), t.getResolvedAt()).toMinutes())
                .sum();
        double avgHours = (totalMinutes / resolvedTickets.size()) / 60.0;
        // Round to one decimal place for a cleaner dashboard number.
        return Math.round(avgHours * 10) / 10.0;
    }

    private UserResponse toUserResponse(User u) {
        return new UserResponse(u.getId(), u.getFullName(), u.getEmail(), u.getRole().name(), u.isActive());
    }
}
