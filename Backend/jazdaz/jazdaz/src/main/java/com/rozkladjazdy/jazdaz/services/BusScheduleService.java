package com.rozkladjazdy.jazdaz.services;


import com.rozkladjazdy.jazdaz.database.entities.BusStop;
import com.rozkladjazdy.jazdaz.database.entities.BusStopTime;
import com.rozkladjazdy.jazdaz.database.entities.BusTrip;
import com.rozkladjazdy.jazdaz.database.repositories.*;
import com.rozkladjazdy.jazdaz.dtos.BusLineStopsResponse;
import com.rozkladjazdy.jazdaz.dtos.BusStopDepartureDto;
import com.rozkladjazdy.jazdaz.dtos.BusStopDeparturesResponse;
import com.rozkladjazdy.jazdaz.dtos.BusStopOnLineDto;
import com.rozkladjazdy.jazdaz.exceptions.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import com.rozkladjazdy.jazdaz.exceptions.BadDataException;
import com.rozkladjazdy.jazdaz.database.entities.BusServiceCalendarDate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class BusScheduleService {

    private static final int GTFS_SERVICE_ADDED = 1;
    private static final int GTFS_SERVICE_REMOVED = 2;

    private final BusLineRepository busLineRepository;
    private final BusTripRepository busTripRepository;
    private final BusStopTimeRepository busStopTimeRepository;
    private final BusServiceCalendarDateRepository busServiceCalendarDateRepository;
    private final BusStopRepository busStopRepository;

    public BusScheduleService(
            BusLineRepository busLineRepository,
            BusTripRepository busTripRepository,
            BusStopTimeRepository busStopTimeRepository,
            BusServiceCalendarDateRepository busServiceCalendarDateRepository,
            BusStopRepository busStopRepository
    ) {
        this.busLineRepository = busLineRepository;
        this.busTripRepository = busTripRepository;
        this.busStopTimeRepository = busStopTimeRepository;
        this.busServiceCalendarDateRepository = busServiceCalendarDateRepository;
        this.busStopRepository = busStopRepository;
    }

    public BusLineStopsResponse getStopsForLine(
            String lineNumber,
            String directionParam,
            LocalDate date
    ) {
        Integer directionId = parseDirection(directionParam);

        if (!busLineRepository.existsByLineNumber(lineNumber)) {
            throw new ResourceNotFoundException(
                    "Nie znaleziono linii autobusowej: " + lineNumber
            );
        }

        Set<Long> activeServiceCalendarIds = findActiveServiceCalendarIds(date);

        if (activeServiceCalendarIds.isEmpty()) {
            throw new ResourceNotFoundException(
                    "Nie znaleziono aktywnego rozkładu dla daty: " + date
            );
        }

        List<BusTrip> trips = busTripRepository.findByLineLineNumberAndDirectionIdAndServiceCalendarIdIn(
                lineNumber,
                directionId,
                activeServiceCalendarIds.stream().toList()
        );

        if (trips.isEmpty()) {
            throw new ResourceNotFoundException(
                    "Nie znaleziono trasy dla linii " + lineNumber
                            + ", kierunku " + directionId
                            + " i daty " + date
            );
        }

        BusTrip representativeTrip = findRepresentativeTripWithMostStops(trips);

        List<BusStopTime> stopTimes = busStopTimeRepository
                .findByTripIdOrderByStopSequenceAsc(representativeTrip.getId());

        List<BusStopOnLineDto> stops = new ArrayList<>();

        for (BusStopTime stopTime : stopTimes) {
            BusStop stop = stopTime.getStop();

            stops.add(new BusStopOnLineDto(
                    stop.getId(),
                    stop.getExternalId(),
                    stop.getStopCode(),
                    stop.getName(),
                    stop.getLatitude(),
                    stop.getLongitude(),
                    stop.getPlatformCode(),
                    stopTime.getStopSequence()
            ));
        }

        return new BusLineStopsResponse(
                lineNumber,
                representativeTrip.getDirectionId(),
                representativeTrip.getHeadsign(),
                representativeTrip.getId(),
                representativeTrip.getExternalId(),
                stops
        );
    }

    public BusStopDeparturesResponse getDeparturesForLineAndStop(
            String lineNumber,
            Long stopId,
            String directionParam,
            LocalDate date
    ) {
        Integer directionId = parseDirection(directionParam);

        if (!busLineRepository.existsByLineNumber(lineNumber)) {
            throw new ResourceNotFoundException(
                    "Nie znaleziono linii autobusowej: " + lineNumber
            );
        }

        BusStop stop = busStopRepository.findById(stopId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Nie znaleziono przystanku o id: " + stopId
                ));

        Set<Long> activeServiceCalendarIds = findActiveServiceCalendarIds(date);

        if (activeServiceCalendarIds.isEmpty()) {
            throw new ResourceNotFoundException(
                    "Nie znaleziono aktywnego rozkładu dla daty: " + date
            );
        }

        List<BusStopTime> stopTimes =
                busStopTimeRepository.findByTripLineLineNumberAndTripDirectionIdAndTripServiceCalendarIdInAndStopIdOrderByDepartureSecondsAsc(
                        lineNumber,
                        directionId,
                        activeServiceCalendarIds.stream().toList(),
                        stopId
                );

        if (stopTimes.isEmpty()) {
            throw new ResourceNotFoundException(
                    "Nie znaleziono odjazdów dla linii " + lineNumber
                            + ", przystanku " + stopId
                            + ", kierunku " + directionId
                            + " i daty " + date
            );
        }

        List<BusStopDepartureDto> departures = new ArrayList<>();

        for (BusStopTime stopTime : stopTimes) {
            BusTrip trip = stopTime.getTrip();

            departures.add(new BusStopDepartureDto(
                    stopTime.getDepartureTime(),
                    stopTime.getArrivalTime(),
                    stopTime.getDepartureSeconds(),
                    stopTime.getStopSequence(),
                    trip.getId(),
                    trip.getExternalId(),
                    trip.getHeadsign()
            ));
        }

        return new BusStopDeparturesResponse(
                lineNumber,
                directionId,
                date,
                stop.getId(),
                stop.getExternalId(),
                stop.getStopCode(),
                stop.getName(),
                stop.getPlatformCode(),
                departures
        );
    }

    private Set<Long> findActiveServiceCalendarIds(LocalDate date) {
        Set<Long> activeIds = new HashSet<>();

        List<BusServiceCalendarDate> calendarDates =
                busServiceCalendarDateRepository.findByDate(date);

        for (BusServiceCalendarDate calendarDate : calendarDates) {
            Long serviceCalendarId = calendarDate.getServiceCalendar().getId();

            if (Objects.equals(calendarDate.getExceptionType(), GTFS_SERVICE_ADDED)) {
                activeIds.add(serviceCalendarId);
            }

            if (Objects.equals(calendarDate.getExceptionType(), GTFS_SERVICE_REMOVED)) {
                activeIds.remove(serviceCalendarId);
            }
        }

        return activeIds;
    }

    private BusTrip findRepresentativeTripWithMostStops(List<BusTrip> trips) {
        BusTrip bestTrip = null;
        long bestStopCount = -1;

        for (BusTrip trip : trips) {
            long stopCount = busStopTimeRepository.countByTripId(trip.getId());

            if (stopCount > bestStopCount) {
                bestTrip = trip;
                bestStopCount = stopCount;
            }
        }

        if (bestTrip == null) {
            throw new ResourceNotFoundException(
                    "Nie znaleziono reprezentatywnego kursu dla tej linii."
            );
        }

        return bestTrip;
    }

    private Integer parseDirection(String directionParam) {
        if (directionParam == null || directionParam.isBlank()) {
            return 0;
        }

        String value = directionParam.trim().toLowerCase();

        return switch (value) {
            case "0", "default", "normal", "forward" -> 0;
            case "1", "opposite", "reverse", "backward" -> 1;
            default -> throw new BadDataException(
                    "Niepoprawny kierunek. Użyj: 0, 1, default albo opposite."
            );
        };
    }
}