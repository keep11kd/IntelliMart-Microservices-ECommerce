package com.intelimart.authservice.controller;

import com.intelimart.authservice.dto.AuthRequest;
import com.intelimart.authservice.dto.AuthResponse;
import com.intelimart.authservice.dto.RegisterRequest;
import com.intelimart.authservice.model.User;
import com.intelimart.authservice.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping; // Make sure this import is present
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "User registration and authentication APIs")
public class AuthRestController {

    @Autowired
    private AuthService authService;

    // --- Registration Endpoint ---
    @Operation(summary = "Register a new user", description = "Creates a new user account with a unique username and email.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "User registered successfully",
                     content = @Content(mediaType = "text/plain", schema = @Schema(type = "string"))),
        @ApiResponse(responseCode = "400", description = "Invalid input or username/email already exists",
                     content = @Content(mediaType = "text/plain", schema = @Schema(type = "string"))),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
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

    // --- Login Endpoint ---
    @Operation(summary = "User login", description = "Authenticates user credentials and returns a JWT token.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Login successful, returns JWT token",
                     content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthResponse.class))),
        @ApiResponse(responseCode = "401", description = "Invalid username or password",
                     content = @Content(mediaType = "text/plain", schema = @Schema(type = "string")))
    })
    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@Valid @RequestBody AuthRequest authRequest) {
        try {
            String jwtToken = authService.loginUser(authRequest.getUsername(), authRequest.getPassword());
            return ResponseEntity.ok(new AuthResponse(jwtToken));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid username or password.");
        }
    }

    // --- Add this new protected endpoint for testing Gateway JWT validation ---
    @Operation(summary = "Validate JWT token", description = "Endpoint to verify if a JWT token is valid. Protected by API Gateway.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Token is valid",
                     content = @Content(mediaType = "text/plain", schema = @Schema(type = "string"))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - token is missing, invalid, or expired")
    })
    @GetMapping("/validate")
    public ResponseEntity<String> validateToken() {
        // If the request reaches here, it means the Gateway has successfully validated the JWT.
        // You can optionally add Spring Security checks here as well if needed.
        return ResponseEntity.ok("Token is valid and authenticated by Auth Service.");
    }
}