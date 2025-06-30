package com.intelimart.authservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
	
	 @Bean
	  public PasswordEncoder passwordEncoder() {
	        return new BCryptPasswordEncoder(); // This bean will be used for password hashing
	    }
	 @Bean
	    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
	        // For Day 4, we're temporarily disabling CSRF and authorizing all requests
	        // to easily test the registration endpoint.
	        // In a real application, you would configure specific authorization rules.
	        http
	            .csrf(csrf -> csrf.disable()) // Disable CSRF for Postman testing (re-enable for production or use tokens)
	            .authorizeHttpRequests(authorize -> authorize
	                .anyRequest().permitAll() // Allow all requests for now (temporarily for registration testing)
	            );
	        return http.build();
	    }
}
