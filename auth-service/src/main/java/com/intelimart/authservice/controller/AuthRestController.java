package com.intelimart.authservice.controller;

import com.intelimart.authservice.dto.RegisterRequest;
import com.intelimart.authservice.model.User;
import com.intelimart.authservice.service.AuthService;
import jakarta.validation.Valid; // For validating the RegisterRequest DTO
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController // Marks this class as a REST Controller
@RequestMapping("/api/auth") // Base path for authentication endpoints
public class AuthRestController {

    @Autowired
    private AuthService authService;

    @PostMapping("/register") // Handles POST requests to /api/auth/register
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        try {
            User registeredUser = authService.registerUser(
                registerRequest.getUsername(),
                registerRequest.getPassword(),
                registerRequest.getEmail()
            );
            // Return a simplified response to avoid sending sensitive data like password hash
            return ResponseEntity.status(HttpStatus.CREATED).body("User registered successfully: " + registeredUser.getUsername());
        } catch (RuntimeException e) {
            // Handle cases where username or email already exists
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            // Catch any other unexpected errors
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred during registration.");
        }
    }
}
