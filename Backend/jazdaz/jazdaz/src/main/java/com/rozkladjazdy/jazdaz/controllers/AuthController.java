package com.rozkladjazdy.jazdaz.controllers;

import com.rozkladjazdy.jazdaz.dtos.RegisterRequestDto;
import com.rozkladjazdy.jazdaz.services.RegistrationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final RegistrationService registrationService;

    public AuthController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @PostMapping("/register")
    public void register(@RequestBody RegisterRequestDto request) {
        boolean isRegistered = registrationService.register(request);

        if (isRegistered) {
            throw new ResponseStatusException(HttpStatus.CREATED);
        }

        throw new ResponseStatusException(HttpStatus.CONFLICT);
    }
}