package com.intelimart.authservice.repository;

import com.intelimart.authservice.model.User;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends CrudRepository<User, Long> {
    // You can add custom query methods here if needed, e.g.:
    // Optional<User> findByUsername(String username);
    // Optional<User> findByEmail(String email);
}