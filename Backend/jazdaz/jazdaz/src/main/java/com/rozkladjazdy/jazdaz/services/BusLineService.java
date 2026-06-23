package com.rozkladjazdy.jazdaz.services;


import com.rozkladjazdy.jazdaz.database.entities.BusLine;
import com.rozkladjazdy.jazdaz.database.repositories.BusLineRepository;
import com.rozkladjazdy.jazdaz.dtos.BusLineDto;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class BusLineService {


    private final BusLineRepository busLineRepository;

    public BusLineService(BusLineRepository busLineRepository) {
        this.busLineRepository = busLineRepository;
    }

    public List<BusLineDto> getAllBusLines() {
        return busLineRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(BusLine::getLineNumber, this::compareLineNumbers))
                .map(this::mapToDto)
                .toList();
    }

    private BusLineDto mapToDto(BusLine line) {
        return new BusLineDto(
                line.getId(),
                line.getExternalId(),
                line.getLineNumber(),
                line.getName()
        );
    }

    private int compareLineNumbers(String first, String second) {
        if (first == null && second == null) {
            return 0;
        }

        if (first == null) {
            return 1;
        }

        if (second == null) {
            return -1;
        }

        boolean firstIsNumber = first.matches("\\d+");
        boolean secondIsNumber = second.matches("\\d+");

        if (firstIsNumber && secondIsNumber) {
            return Integer.compare(
                    Integer.parseInt(first),
                    Integer.parseInt(second)
            );
        }

        if (firstIsNumber) {
            return -1;
        }

        if (secondIsNumber) {
            return 1;
        }

        return first.compareToIgnoreCase(second);
    }
}