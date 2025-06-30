package com.intelimart.authservice.controller;

import com.intelimart.authservice.dto.AuthRequest; // Import AuthRequest
import com.intelimart.authservice.dto.AuthResponse;
import com.intelimart.authservice.dto.RegisterRequest;
import com.intelimart.authservice.model.User;
import com.intelimart.authservice.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthRestController {

    @Autowired
    private AuthService authService;

    // --- Registration Endpoint (from Day 4) ---
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        try {
            User registeredUser = authService.registerUser(
                registerRequest.getUsername(),
                registerRequest.getPassword(),
                registerRequest.getEmail()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body("User registered successfully: " + registeredUser.getUsername());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred during registration.");
        }
    }

    // --- New Login Endpoint ---
    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@Valid @RequestBody AuthRequest authRequest) {
        try {
            String jwtToken = authService.loginUser(authRequest.getUsername(), authRequest.getPassword());
            // Return the JWT token in a proper JSON response (e.g., {"token": "your.jwt.token"})
            return ResponseEntity.ok(new AuthResponse(jwtToken));
        } catch (Exception e) {
            // Catch authentication exceptions (e.g., BadCredentialsException)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid username or password.");
        }
    }
}