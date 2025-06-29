package com.intelimart.authservice.model;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true)
    private String email;

    // ManyToMany relationship with Role
    @ManyToMany(fetch = FetchType.EAGER) // Often EAGER for roles, but consider LAZY for performance in large apps
    @JoinTable(
        name = "user_roles", // This is the name of the join table
        joinColumns = @JoinColumn(name = "user_id"), // Column in user_roles that refers to user_id
        inverseJoinColumns = @JoinColumn(name = "role_id") // Column in user_roles that refers to role_id
    )
    private Set<Role> roles = new HashSet<>();

    public User() {
    }

    public User(String username, String password, String email) { // Constructor adjusted
        this.username = username;
        this.password = password;
        this.email = email;
        this.roles = new HashSet<>(); // Initialize roles
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public void setRoles(Set<Role> roles) {
        this.roles = roles;
    }

    // Helper method to add a single role
    public void addRole(Role role) {
        this.roles.add(role);
        role.getUsers().add(this);
    }

    // Helper method to remove a single role
    public void removeRole(Role role) {
        this.roles.remove(role);
        role.getUsers().remove(this);
    }
}