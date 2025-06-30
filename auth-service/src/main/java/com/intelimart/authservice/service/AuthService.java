package com.intelimart.authservice.service;

import com.intelimart.authservice.model.User;
import com.intelimart.authservice.model.Role;
import com.intelimart.authservice.repository.UserRepository;
import com.intelimart.authservice.repository.RoleRepository;
import com.intelimart.authservice.util.JwtUtil; // Import JwtUtil
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager; // Import AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken; // Import Token
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails; // Import UserDetails
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired // Inject AuthenticationManager
    private AuthenticationManager authenticationManager;

    @Autowired // Inject JwtUtil
    private JwtUtil jwtUtil;

    // --- Registration Logic (from Day 4, no changes needed here unless you like) ---
    public User registerUser(String username, String password, String email) {
        if (userRepository.findByUsername(username).isPresent()) {
            throw new RuntimeException("Username already taken: " + username);
        }
        if (userRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("Email already registered: " + email);
        }

        User newUser = new User(username, passwordEncoder.encode(password), email);

        Optional<Role> userRoleOptional = roleRepository.findByName("ROLE_USER");
        Role userRole;

        if (userRoleOptional.isEmpty()) {
            userRole = new Role("ROLE_USER");
            roleRepository.save(userRole);
        } else {
            userRole = userRoleOptional.get();
        }

        Set<Role> roles = new HashSet<>();
        roles.add(userRole);
        newUser.setRoles(roles);

        return userRepository.save(newUser);
    }

    // --- New Login Logic ---
    public String loginUser(String username, String password) {
        // Authenticate the user using Spring Security's AuthenticationManager
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password)
        );

        // Set the authenticated object in the SecurityContextHolder
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Generate JWT token
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return jwtUtil.generateToken(userDetails);
    }
}