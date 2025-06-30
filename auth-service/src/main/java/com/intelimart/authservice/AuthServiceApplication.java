package com.intelimart.authservice;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.intelimart.authservice.model.Role;
import com.intelimart.authservice.repository.RoleRepository;

@SpringBootApplication
public class AuthServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(AuthServiceApplication.class, args);
	}

	@Autowired
	private RoleRepository roleRepository;

	@Bean
	public CommandLineRunner createDefaultRoles() {
		return args -> {
			if (roleRepository.findByName("ROLE_USER").isEmpty()) {
				roleRepository.save(new Role("ROLE_USER"));
				System.out.println("Created ROLE_USER role.");
			}
			if (roleRepository.findByName("ROLE_ADMIN").isEmpty()) { // Optional: add an admin role too
				roleRepository.save(new Role("ROLE_ADMIN"));
				System.out.println("Created ROLE_ADMIN role.");
			}
		};
	}
}
