package com.rozkladjazdy.jazdaz.database.importers;

import com.rozkladjazdy.jazdaz.database.entities.UserEntity;
import com.rozkladjazdy.jazdaz.database.repositories.UserRepository;
import com.rozkladjazdy.jazdaz.enums.UserRole;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Order(3)
public class AdminAccountInitializer implements ApplicationRunner {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;

    public AdminAccountInitializer(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${localmobility.admin.email:admin@localmobility.pl}") String adminEmail,
            @Value("${localmobility.admin.password:admin}") String adminPassword
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (adminEmail == null || adminEmail.trim().isEmpty()) {
            return;
        }

        if (adminPassword == null || adminPassword.trim().isEmpty()) {
            return;
        }

        UserEntity admin = userRepository.findByEmail(adminEmail)
                .orElseGet(UserEntity::new);

        admin.setEmail(adminEmail);
        admin.setHashedPassword(passwordEncoder.encode(adminPassword));
        admin.setRole(UserRole.ADMIN);

        userRepository.save(admin);
    }
}