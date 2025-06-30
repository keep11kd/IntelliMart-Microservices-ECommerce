package com.intelimart.authservice.repository;

import com.intelimart.authservice.model.User;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends CrudRepository<User, Long> {
	// This is the method you need for findByUsername
    Optional<User> findByUsername(String username);

    // Also ensure findByEmail is present if you copied it all
    Optional<User> findByEmail(String email);
}