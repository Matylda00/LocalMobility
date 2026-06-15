package com.rozkladjazdy.jazdaz.services;

import com.rozkladjazdy.jazdaz.database.entities.UserEntity;
import com.rozkladjazdy.jazdaz.database.repositories.UserRepository;
import com.rozkladjazdy.jazdaz.dtos.RegisterRequestDto;
import com.rozkladjazdy.jazdaz.enums.UserRole;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class RegistrationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public RegistrationService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public boolean register(RegisterRequestDto request) {
        if (userRepository.findByEmail(request.email()).isPresent()) {
            return false;
        }

        UserEntity user = new UserEntity();
        user.setEmail(request.email());
        user.setHashedPassword(passwordEncoder.encode(request.password()));
        user.setRole(UserRole.USER);

        userRepository.save(user);

        return true;
    }
}