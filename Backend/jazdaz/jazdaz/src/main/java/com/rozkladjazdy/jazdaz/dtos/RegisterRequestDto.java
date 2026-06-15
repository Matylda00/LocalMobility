package com.rozkladjazdy.jazdaz.dtos;

public record RegisterRequestDto(
        String email,
        String password
) {
}