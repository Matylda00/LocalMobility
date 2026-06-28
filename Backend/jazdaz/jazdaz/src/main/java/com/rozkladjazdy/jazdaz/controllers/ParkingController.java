package com.rozkladjazdy.jazdaz.controllers;



import com.rozkladjazdy.jazdaz.dtos.ParkingResponse;
import com.rozkladjazdy.jazdaz.services.WarsawParkingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ParkingController {

    private final WarsawParkingService parkingService;

    public ParkingController(WarsawParkingService parkingService) {
        this.parkingService = parkingService;
    }

    @GetMapping("/api/parkings")
    public ParkingResponse getParkings() {
        return parkingService.getParkings();
    }
}