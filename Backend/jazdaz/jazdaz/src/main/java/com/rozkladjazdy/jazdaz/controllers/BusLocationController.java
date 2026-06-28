package com.rozkladjazdy.jazdaz.controllers;

import com.rozkladjazdy.jazdaz.dtos.BusLocationResponse;
import com.rozkladjazdy.jazdaz.services.WarsawBusLocationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
public class BusLocationController {

    private final WarsawBusLocationService warsawBusLocationService;

    public BusLocationController(WarsawBusLocationService warsawBusLocationService) {
        this.warsawBusLocationService = warsawBusLocationService;
    }

    @GetMapping("/api/bus-locations")
    public BusLocationResponse getBusLocations() {
        return warsawBusLocationService.getBusLocations();
    }
}