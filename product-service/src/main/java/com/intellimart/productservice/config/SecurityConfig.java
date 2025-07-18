package com.intellimart.productservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer; // Import for Customizer.withDefaults()
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity // Enables Spring Security's web security support
@EnableMethodSecurity // Enables method-level security with @PreAuthorize
public class SecurityConfig {

    // Defines in-memory users for demonstration. In a real app, use a DB.
    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        // Create a regular user
        UserDetails user = User.withUsername("user")
                .password(passwordEncoder.encode("password"))
                .roles("USER")
                .build();

        // Create an admin user
        UserDetails admin = User.withUsername("admin")
                .password(passwordEncoder.encode("adminpass"))
                .roles("ADMIN", "USER") // Admin also has USER role
                .build();

        return new InMemoryUserDetailsManager(user, admin);
    }

    // Configures password encoder for hashing passwords
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Configures the security filter chain (authorization rules)
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // Disable CSRF for API endpoints (common for stateless REST APIs)
            .authorizeHttpRequests(authorize -> authorize
                // --- Explicitly permit access to Swagger UI and API Docs endpoints ---
                // These paths must be accessible without authentication for Swagger UI to load
                .requestMatchers(
                    "/swagger-ui.html",           // Main Swagger UI page
                    "/swagger-ui/**",             // Swagger UI static resources (JS, CSS, images)
                    "/v3/api-docs/**",            // Default OpenAPI 3 documentation endpoint
                    "/api-docs",                  // Custom API docs path from application.properties
                    "/api-docs/**",               // Sub-paths under the custom API docs path
                    "/webjars/**"                 // Resources served by webjars (e.g., for Swagger UI)
                ).permitAll()

                // --- Permit access to other public endpoints ---
                // Public GET requests for product and category APIs (viewing products/categories)
                .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/categories/**").permitAll()
                // Public access to static image serving
                .requestMatchers("/images/**").permitAll()
                // Public access to H2 console (for development only)
                .requestMatchers("/h2-console/**").permitAll()
                // Permit access to Spring Boot Actuator endpoints (if enabled and desired to be public)
                .requestMatchers("/actuator/**").permitAll()

                // --- All other requests require authentication ---
                // Any request not explicitly permitted above will require authentication
                .anyRequest().authenticated()
            )
            .headers(headers -> headers.frameOptions().disable()) // Required for H2 console to work in a frame
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS) // Use stateless sessions for REST APIs
            )
            .httpBasic(Customizer.withDefaults()); // Enable HTTP Basic authentication for demonstration

        return http.build();
    }
}