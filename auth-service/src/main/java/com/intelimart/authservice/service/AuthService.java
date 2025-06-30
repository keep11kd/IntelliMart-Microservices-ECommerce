package com.intelimart.authservice.service;

import com.intelimart.authservice.model.User;
import com.intelimart.authservice.model.Role;
import com.intelimart.authservice.repository.UserRepository;
import com.intelimart.authservice.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Service // Marks this class as a Spring Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository; // To get default user role

    @Autowired
    private PasswordEncoder passwordEncoder; // The BCryptPasswordEncoder bean

    public User registerUser(String username, String password, String email) {
        // Check if username or email already exists
        if (userRepository.findByUsername(username).isPresent()) {
            throw new RuntimeException("Username already taken: " + username);
        }
        if (userRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("Email already registered: " + email);
        }

        // Create new user instance
        User newUser = new User(username, passwordEncoder.encode(password), email); // Encode password!

        // Assign default role (e.g., ROLE_USER)
        Optional<Role> userRoleOptional = roleRepository.findByName("ROLE_USER");
        Role userRole;

        if (userRoleOptional.isEmpty()) {
            // If "ROLE_USER" does not exist, create it.
            // In a real application, you might pre-populate roles on application startup.
            userRole = new Role("ROLE_USER");
            roleRepository.save(userRole); // Save the new role
        } else {
            userRole = userRoleOptional.get();
        }

        Set<Role> roles = new HashSet<>();
        roles.add(userRole);
        newUser.setRoles(roles);

        return userRepository.save(newUser); // Save the new user to the database
    }

    // You might add login logic here later
}