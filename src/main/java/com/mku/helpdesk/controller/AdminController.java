package com.mku.helpdesk.controller;

import com.mku.helpdesk.dto.AnalyticsResponse;
import com.mku.helpdesk.dto.CreateStaffRequest;
import com.mku.helpdesk.dto.UserResponse;
import com.mku.helpdesk.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @PostMapping("/users")
    public ResponseEntity<UserResponse> createStaff(@Valid @RequestBody CreateStaffRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createStaff(request));
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserResponse>> listStaff() {
        return ResponseEntity.ok(adminService.listStaff());
    }

    @PutMapping("/users/{id}/toggle")
    public ResponseEntity<UserResponse> toggleActive(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.toggleActive(id));
    }

    @GetMapping("/analytics")
    public ResponseEntity<AnalyticsResponse> analytics() {
        return ResponseEntity.ok(adminService.getAnalytics());
    }
}
