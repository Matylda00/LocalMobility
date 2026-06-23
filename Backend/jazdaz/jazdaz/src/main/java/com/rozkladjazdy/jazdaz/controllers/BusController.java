package com.rozkladjazdy.jazdaz.controllers;


import com.rozkladjazdy.jazdaz.dtos.BusLineDto;
import com.rozkladjazdy.jazdaz.dtos.BusLineStopsResponse;
import com.rozkladjazdy.jazdaz.dtos.BusStopDeparturesResponse;
import com.rozkladjazdy.jazdaz.services.BusLineService;
import com.rozkladjazdy.jazdaz.services.BusScheduleService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping
public class BusController {

    private final BusScheduleService busScheduleService;
    private final BusLineService busLineService;
    public BusController(
            BusScheduleService busScheduleService,
            BusLineService busLineService) {
        this.busScheduleService = busScheduleService;
        this.busLineService = busLineService;
    }

    @GetMapping("/api/bus-lines/{lineNumber}/stops")
    public BusLineStopsResponse getStopsForLine(
            @PathVariable String lineNumber,
            @RequestParam(defaultValue = "default") String direction,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date
    ) {
        LocalDate selectedDate = date != null ? date : LocalDate.now();

        return busScheduleService.getStopsForLine(
                lineNumber,
                direction,
                selectedDate
        );
    }
    @GetMapping("/api/bus-lines/{lineNumber}/stops/{stopId}/departures")
    public BusStopDeparturesResponse getDeparturesForLineAndStop(
            @PathVariable String lineNumber,
            @PathVariable Long stopId,
            @RequestParam(defaultValue = "default") String direction,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date
    ) {
        LocalDate selectedDate = date != null ? date : LocalDate.now();

        return busScheduleService.getDeparturesForLineAndStop(
                lineNumber,
                stopId,
                direction,
                selectedDate
        );
    }
    @GetMapping("/api/bus-lines")
    public List<BusLineDto> getAllBusLines() {
        return busLineService.getAllBusLines();
    }
}
