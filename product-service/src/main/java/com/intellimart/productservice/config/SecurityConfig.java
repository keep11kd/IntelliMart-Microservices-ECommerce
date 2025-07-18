package com.intellimart.productservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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
            .csrf(csrf -> csrf.disable()) // Disable CSRF for API endpoints
            .authorizeHttpRequests(authorize -> authorize
                // Allow unauthenticated access to /api/products GET requests (viewing products)
                .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()
                // Allow unauthenticated access to /api/categories GET requests
                .requestMatchers(HttpMethod.GET, "/api/categories/**").permitAll()
                // Require authentication for other requests (e.g., Postman access to login)
                .requestMatchers("/h2-console/**").permitAll() // Allow H2 console access for dev
                .anyRequest().authenticated() // All other requests need authentication
            )
            .headers(headers -> headers.frameOptions().disable()) // Required for H2 console
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS) // Use stateless sessions for REST APIs
            )
            .httpBasic(httpBasic -> {}); // Enable HTTP Basic authentication for demonstration

        return http.build();
    }
 
    
}