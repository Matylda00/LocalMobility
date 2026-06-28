package com.rozkladjazdy.jazdaz.controllers;

import com.rozkladjazdy.jazdaz.dtos.BikeStationResponse;
import com.rozkladjazdy.jazdaz.services.VeturiloBikeStationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BikeStationController {

    private final VeturiloBikeStationService veturiloBikeStationService;

    public BikeStationController(VeturiloBikeStationService veturiloBikeStationService) {
        this.veturiloBikeStationService = veturiloBikeStationService;
    }

    @GetMapping("/api/bike-stations")
    public BikeStationResponse getBikeStations() {
        return veturiloBikeStationService.getBikeStations();
    }
}