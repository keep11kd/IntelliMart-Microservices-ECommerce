package com.intelimart.authservice.config;

import com.intelimart.authservice.service.UserDetailsServiceImpl; // Import your UserDetailsService
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration; // Import this
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.Customizer; // Added for basic HTTP Basic Auth configuration

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private UserDetailsServiceImpl userDetailsService; // Inject your custom UserDetailsService
    
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Expose AuthenticationManager as a Bean
    // This is crucial for AuthService to use authenticationManager.authenticate()
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        // This is the correct way to expose AuthenticationManager in Spring Boot 3+
        return authenticationConfiguration.getAuthenticationManager();
    }

    // Configure DaoAuthenticationProvider to use our UserDetailsService and PasswordEncoder
    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService); // Set your custom UserDetailsService
        provider.setPasswordEncoder(passwordEncoder());    // Set your PasswordEncoder
        return provider;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // Disable CSRF for simplicity in API testing for now
            .authorizeHttpRequests(authorize -> authorize
                // Allow access to register and login endpoints without authentication
                .requestMatchers("/api/auth/register", "/api/auth/login").permitAll()
                // The /validate endpoint now requires authentication
                .requestMatchers("/api/auth/validate").authenticated()
                // All other requests require authentication (will be fully implemented later)
                .anyRequest().authenticated()
            )
            .httpBasic(Customizer.withDefaults()); // Keep this for now for basic debugging if needed

        return http.build();
    }
}