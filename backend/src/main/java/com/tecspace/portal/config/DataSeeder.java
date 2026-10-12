package com.tecspace.portal.config;

import com.tecspace.portal.model.User;
import com.tecspace.portal.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Creates the first admin account on startup if it does not exist yet. */
@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${tecspace.admin.email}")
    private String adminEmail;

    @Value("${tecspace.admin.password}")
    private String adminPassword;

    public DataSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.existsByUniversityEmail(adminEmail)) return;
        User admin = new User();
        admin.setUniversityEmail(adminEmail);
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setFirstName("TecSpace");
        admin.setLastName("Admin");
        admin.setMobileNumber("+0000000000");
        admin.setFaculty("ADMIN");
        admin.setSession("N/A");
        admin.setRole("ROLE_ADMIN");
        userRepository.save(admin);
        System.out.println(">>> TecSpace admin created: " + adminEmail);
    }
}
