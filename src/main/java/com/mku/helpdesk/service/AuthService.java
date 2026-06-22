package com.mku.helpdesk.service;

import com.mku.helpdesk.dto.LoginRequest;
import com.mku.helpdesk.dto.LoginResponse;
import com.mku.helpdesk.dto.RegisterRequest;
import com.mku.helpdesk.model.Role;
import com.mku.helpdesk.model.User;
import com.mku.helpdesk.repository.UserRepository;
import com.mku.helpdesk.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Transactional
    public void register(RegisterRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Password and confirmation do not match.");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("This email address is already registered.");
        }
        if (userRepository.existsByStudentNumber(request.getStudentNumber())) {
            throw new IllegalArgumentException("This student number is already registered.");
        }

        User user = new User();
        user.setStudentNumber(request.getStudentNumber());
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        // THE LINE THAT MATTERS MOST IN THIS FILE: encode() before save().
        // Skip this and you store the plaintext password in the database —
        // a serious security failure, and login will ALSO break afterwards,
        // because passwordEncoder.matches() will never match a plaintext
        // value against itself the way bcrypt expects to compare.
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.STUDENT); // hardcoded — see RegisterRequest comment for why

        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                // Same generic message whether the email doesn't exist or the
                // password is wrong (checked below) — see GlobalExceptionHandler
                // comment for why this matters.
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password."));

        if (!user.isActive()) {
            throw new BadCredentialsException("This account has been deactivated. Contact ICT support.");
        }

        // NEVER compare passwordHash.equals(rawPassword) — that compares a
        // bcrypt hash to plaintext and will always be false. matches() does
        // the hashing-and-comparison correctly under the hood.
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password.");
        }

        String token = jwtUtil.generateToken(user);
        return new LoginResponse(token, user.getRole().name(), user.getFullName(), user.getId());
    }
}
