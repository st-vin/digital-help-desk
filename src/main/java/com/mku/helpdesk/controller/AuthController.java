package com.mku.helpdesk.controller;

import com.mku.helpdesk.dto.LoginRequest;
import com.mku.helpdesk.dto.LoginResponse;
import com.mku.helpdesk.dto.RegisterRequest;
import com.mku.helpdesk.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Account created successfully. You can now log in."));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        try {
            System.out.println("Login endpoint hit for: " + request.getEmail());
        return ResponseEntity.ok(authService.login(request));
        } catch (Throwable t) {
            t.printStackTrace();
            throw t;
        }
    }
}
