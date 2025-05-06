package com.group8.alomilktea.controller.api;

import com.group8.alomilktea.entity.Roles;
import com.group8.alomilktea.entity.User;
import com.group8.alomilktea.service.IUserService;
import com.group8.alomilktea.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthApiController {
    private final IUserService userService;
    private final JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestParam String email, @RequestParam String password) {
        try {
            Optional<User> userOpt = userService.findByEmail(email);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                if (password.equals(user.getPasswordHash())) {
                    // Set user status to online
                    userService.saveUserOnline(user);
                    
                    // Generate JWT token
                    String token = jwtUtil.generateToken(user);
                    
                    Map<String, Object> response = new HashMap<>();
                    response.put("userId", user.getUserId());
                    response.put("email", user.getEmail());
                    response.put("fullName", user.getFullName());
                    response.put("roles", user.getRoles().stream()
                            .map(role -> role.getRole().toString())
                            .collect(Collectors.toSet()));
                    response.put("message", "Login successful");
                    response.put("token", token);
                    
                    return ResponseEntity.ok(response);
                }
            }
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid credentials"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Login failed: " + e.getMessage()));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam String fullName,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String address) {
        try {
            // Check if email already exists
            Optional<User> existingUser = userService.findByEmail(email);
            if (existingUser.isPresent()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Email already registered"));
            }

            // Create new user
            User newUser = User.builder()
                    .email(email)
                    .passwordHash(password) // Note: In production, password should be hashed
                    .fullName(fullName)
                    .phone(phone)
                    .address(address)
                    .isAdmin(false)
                    .active(true)
                    .isEnabled(true)
                    .build();
            
            userService.save(newUser);

            return ResponseEntity.ok(Map.of(
                "message", "Registration successful",
                "userId", newUser.getUserId(),
                "email", newUser.getEmail()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Registration failed: " + e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        try {
            User user = userService.getUserLogged();
            if (user != null) {
                userService.disconnect(user);
                return ResponseEntity.ok(Map.of("message", "Logout successful"));
            }
            return ResponseEntity.badRequest().body(Map.of("error", "No user logged in"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Logout failed: " + e.getMessage()));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        try {
            User user = userService.getUserLogged();
            if (user != null) {
                Map<String, Object> response = new HashMap<>();
                response.put("userId", user.getUserId());
                response.put("email", user.getEmail());
                response.put("fullName", user.getFullName());
                response.put("phone", user.getPhone());
                response.put("address", user.getAddress());
                response.put("roles", user.getRoles().stream()
                        .map(role -> role.getRole().toString())
                        .collect(Collectors.toSet()));
                return ResponseEntity.ok(response);
            }
            return ResponseEntity.badRequest().body(Map.of("error", "No user logged in"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to get user info: " + e.getMessage()));
        }
    }
} 