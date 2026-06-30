package com.rozkladjazdy.jazdaz.services;

import com.rozkladjazdy.jazdaz.database.entities.UserEntity;
import com.rozkladjazdy.jazdaz.database.repositories.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminService {
    private final UserRepository userRepository;

    public AdminService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<String> getCreatedEmails() {
        return userRepository.findAll().stream().map(UserEntity::getEmail).toList();
    }
}