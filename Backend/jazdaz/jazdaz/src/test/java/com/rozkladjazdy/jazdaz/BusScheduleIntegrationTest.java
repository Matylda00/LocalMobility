package com.rozkladjazdy.jazdaz;


import com.rozkladjazdy.jazdaz.database.entities.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class BusScheduleIntegrationTest {

    private static final LocalDate ACTIVE_DATE = LocalDate.of(2026, 6, 22);
    private static final LocalDate INACTIVE_DATE = LocalDate.of(2026, 6, 23);

    private static final int GTFS_SERVICE_ADDED = 1;
    private static final int GTFS_SERVICE_REMOVED = 2;
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManager entityManager;

    private BusLine line105;
    private BusServiceCalendar calendar;

    private BusStop stopA;
    private BusStop stopB;
    private BusStop stopC;
    private BusStop stopD;

    @BeforeEach
    void setUp() {
        line105 = createLine("route-105", "105", "Linia 105");

        calendar = createCalendar("service-105");
        createCalendarDate(calendar, ACTIVE_DATE, GTFS_SERVICE_ADDED);

        stopA = createStop(
                "stop-a",
                "1001",
                "Rondo Daszyńskiego",
                "01",
                "52.2300000",
                "20.9840000"
        );

        stopB = createStop(
                "stop-b",
                "1002",
                "Muzeum Powstania Warszawskiego",
                "02",
                "52.2320000",
                "20.9810000"
        );

        stopC = createStop(
                "stop-c",
                "1003",
                "Pl. Zawiszy",
                "03",
                "52.2260000",
                "20.9900000"
        );

        stopD = createStop(
                "stop-d",
                "1004",
                "Browarna",
                "04",
                "52.2440000",
                "21.0170000"
        );

        BusTrip defaultShortTrip = createTrip(
                "trip-105-default-short",
                line105,
                calendar,
                "Browarna",
                0
        );

        createStopTime(defaultShortTrip, stopA, 1, "08:00:00", "08:00:00");
        createStopTime(defaultShortTrip, stopB, 2, "08:03:00", "08:03:00");

        BusTrip defaultLongTrip = createTrip(
                "trip-105-default-long",
                line105,
                calendar,
                "Browarna",
                0
        );

        createStopTime(defaultLongTrip, stopA, 1, "09:00:00", "09:00:00");
        createStopTime(defaultLongTrip, stopB, 2, "09:03:00", "09:03:00");
        createStopTime(defaultLongTrip, stopC, 3, "09:07:00", "09:07:00");

        BusTrip oppositeTrip = createTrip(
                "trip-105-opposite",
                line105,
                calendar,
                "Rondo Daszyńskiego",
                1
        );

        createStopTime(oppositeTrip, stopD, 1, "10:00:00", "10:00:00");
        createStopTime(oppositeTrip, stopC, 2, "10:05:00", "10:05:00");
        createStopTime(oppositeTrip, stopB, 3, "10:09:00", "10:09:00");

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void shouldReturnStopsForDefaultDirectionAndGivenDate() throws Exception {
        mockMvc.perform(get("/api/bus-lines/105/stops")
                        .param("direction", "default")
                        .param("date", ACTIVE_DATE.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lineNumber", is("105")))
                .andExpect(jsonPath("$.directionId", is(0)))
                .andExpect(jsonPath("$.headsign", is("Browarna")))
                .andExpect(jsonPath("$.representativeTripExternalId", is("trip-105-default-long")))
                .andExpect(jsonPath("$.stops", hasSize(3)))
                .andExpect(jsonPath("$.stops[0].name", is("Rondo Daszyńskiego")))
                .andExpect(jsonPath("$.stops[0].stopSequence", is(1)))
                .andExpect(jsonPath("$.stops[1].name", is("Muzeum Powstania Warszawskiego")))
                .andExpect(jsonPath("$.stops[1].stopSequence", is(2)))
                .andExpect(jsonPath("$.stops[2].name", is("Pl. Zawiszy")))
                .andExpect(jsonPath("$.stops[2].stopSequence", is(3)));
    }

    @Test
    void shouldReturnStopsForOppositeDirectionAndGivenDate() throws Exception {
        mockMvc.perform(get("/api/bus-lines/105/stops")
                        .param("direction", "opposite")
                        .param("date", ACTIVE_DATE.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lineNumber", is("105")))
                .andExpect(jsonPath("$.directionId", is(1)))
                .andExpect(jsonPath("$.headsign", is("Rondo Daszyńskiego")))
                .andExpect(jsonPath("$.representativeTripExternalId", is("trip-105-opposite")))
                .andExpect(jsonPath("$.stops", hasSize(3)))
                .andExpect(jsonPath("$.stops[0].name", is("Browarna")))
                .andExpect(jsonPath("$.stops[0].stopSequence", is(1)))
                .andExpect(jsonPath("$.stops[1].name", is("Pl. Zawiszy")))
                .andExpect(jsonPath("$.stops[1].stopSequence", is(2)))
                .andExpect(jsonPath("$.stops[2].name", is("Muzeum Powstania Warszawskiego")))
                .andExpect(jsonPath("$.stops[2].stopSequence", is(3)));
    }

    @Test
    void shouldReturn404WhenLineDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/bus-lines/999/stops")
                        .param("direction", "default")
                        .param("date", ACTIVE_DATE.toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn400WhenDirectionIsInvalid() throws Exception {
        mockMvc.perform(get("/api/bus-lines/105/stops")
                        .param("direction", "wrong")
                        .param("date", ACTIVE_DATE.toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn404WhenNoScheduleExistsForGivenDate() throws Exception {
        mockMvc.perform(get("/api/bus-lines/105/stops")
                        .param("direction", "default")
                        .param("date", INACTIVE_DATE.toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnDeparturesForSelectedStopDefaultDirectionAndGivenDate() throws Exception {
        mockMvc.perform(get("/api/bus-lines/105/stops/{stopId}/departures", stopA.getId())
                        .param("direction", "default")
                        .param("date", ACTIVE_DATE.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lineNumber", is("105")))
                .andExpect(jsonPath("$.directionId", is(0)))
                .andExpect(jsonPath("$.date", is(ACTIVE_DATE.toString())))
                .andExpect(jsonPath("$.stopId", is(stopA.getId().intValue())))
                .andExpect(jsonPath("$.stopExternalId", is("stop-a")))
                .andExpect(jsonPath("$.stopCode", is("1001")))
                .andExpect(jsonPath("$.stopName", is("Rondo Daszyńskiego")))
                .andExpect(jsonPath("$.platformCode", is("01")))
                .andExpect(jsonPath("$.departures", hasSize(2)))
                .andExpect(jsonPath("$.departures[0].departureTime", is("08:00:00")))
                .andExpect(jsonPath("$.departures[0].arrivalTime", is("08:00:00")))
                .andExpect(jsonPath("$.departures[0].departureSeconds", is(28800)))
                .andExpect(jsonPath("$.departures[0].stopSequence", is(1)))
                .andExpect(jsonPath("$.departures[0].tripExternalId", is("trip-105-default-short")))
                .andExpect(jsonPath("$.departures[0].headsign", is("Browarna")))
                .andExpect(jsonPath("$.departures[1].departureTime", is("09:00:00")))
                .andExpect(jsonPath("$.departures[1].arrivalTime", is("09:00:00")))
                .andExpect(jsonPath("$.departures[1].departureSeconds", is(32400)))
                .andExpect(jsonPath("$.departures[1].stopSequence", is(1)))
                .andExpect(jsonPath("$.departures[1].tripExternalId", is("trip-105-default-long")))
                .andExpect(jsonPath("$.departures[1].headsign", is("Browarna")));
    }

    @Test
    void shouldReturnDeparturesForSelectedStopOppositeDirectionAndGivenDate() throws Exception {
        mockMvc.perform(get("/api/bus-lines/105/stops/{stopId}/departures", stopB.getId())
                        .param("direction", "opposite")
                        .param("date", ACTIVE_DATE.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lineNumber", is("105")))
                .andExpect(jsonPath("$.directionId", is(1)))
                .andExpect(jsonPath("$.date", is(ACTIVE_DATE.toString())))
                .andExpect(jsonPath("$.stopId", is(stopB.getId().intValue())))
                .andExpect(jsonPath("$.stopExternalId", is("stop-b")))
                .andExpect(jsonPath("$.stopName", is("Muzeum Powstania Warszawskiego")))
                .andExpect(jsonPath("$.departures", hasSize(1)))
                .andExpect(jsonPath("$.departures[0].departureTime", is("10:09:00")))
                .andExpect(jsonPath("$.departures[0].arrivalTime", is("10:09:00")))
                .andExpect(jsonPath("$.departures[0].departureSeconds", is(36540)))
                .andExpect(jsonPath("$.departures[0].stopSequence", is(3)))
                .andExpect(jsonPath("$.departures[0].tripExternalId", is("trip-105-opposite")))
                .andExpect(jsonPath("$.departures[0].headsign", is("Rondo Daszyńskiego")));
    }

    @Test
    void shouldNotMixDirectionsWhenReturningDepartures() throws Exception {
        mockMvc.perform(get("/api/bus-lines/105/stops/{stopId}/departures", stopB.getId())
                        .param("direction", "default")
                        .param("date", ACTIVE_DATE.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.directionId", is(0)))
                .andExpect(jsonPath("$.departures", hasSize(2)))
                .andExpect(jsonPath("$.departures[0].tripExternalId", is("trip-105-default-short")))
                .andExpect(jsonPath("$.departures[0].departureTime", is("08:03:00")))
                .andExpect(jsonPath("$.departures[1].tripExternalId", is("trip-105-default-long")))
                .andExpect(jsonPath("$.departures[1].departureTime", is("09:03:00")));
    }

    @Test
    void shouldReturnDeparturesOrderedByDepartureSeconds() throws Exception {
        mockMvc.perform(get("/api/bus-lines/105/stops/{stopId}/departures", stopB.getId())
                        .param("direction", "default")
                        .param("date", ACTIVE_DATE.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.departures", hasSize(2)))
                .andExpect(jsonPath("$.departures[0].departureSeconds", is(28980)))
                .andExpect(jsonPath("$.departures[1].departureSeconds", is(32580)));
    }

    @Test
    void shouldReturn404WhenDeparturesForStopDoNotExistInSelectedDirection() throws Exception {
        mockMvc.perform(get("/api/bus-lines/105/stops/{stopId}/departures", stopD.getId())
                        .param("direction", "default")
                        .param("date", ACTIVE_DATE.toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn404WhenStopDoesNotExistForDeparturesEndpoint() throws Exception {
        mockMvc.perform(get("/api/bus-lines/105/stops/{stopId}/departures", 999999L)
                        .param("direction", "default")
                        .param("date", ACTIVE_DATE.toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn404WhenLineDoesNotExistForDeparturesEndpoint() throws Exception {
        mockMvc.perform(get("/api/bus-lines/999/stops/{stopId}/departures", stopA.getId())
                        .param("direction", "default")
                        .param("date", ACTIVE_DATE.toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn400WhenDirectionIsInvalidForDeparturesEndpoint() throws Exception {
        mockMvc.perform(get("/api/bus-lines/105/stops/{stopId}/departures", stopA.getId())
                        .param("direction", "sideways")
                        .param("date", ACTIVE_DATE.toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn404WhenNoScheduleExistsForGivenDateForDeparturesEndpoint() throws Exception {
        mockMvc.perform(get("/api/bus-lines/105/stops/{stopId}/departures", stopA.getId())
                        .param("direction", "default")
                        .param("date", INACTIVE_DATE.toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldUseOnlyCalendarDatesAndIgnoreWeekdayFlagsForLineStops() throws Exception {
        BusLine line106 = createLine("route-106", "106", "Linia 106");

        BusServiceCalendar weekdayCalendarWithoutCalendarDate = createCalendar("service-106-weekday-flags-only");
        weekdayCalendarWithoutCalendarDate.setMonday(true);
        weekdayCalendarWithoutCalendarDate.setTuesday(true);
        weekdayCalendarWithoutCalendarDate.setWednesday(true);
        weekdayCalendarWithoutCalendarDate.setThursday(true);
        weekdayCalendarWithoutCalendarDate.setFriday(true);
        weekdayCalendarWithoutCalendarDate.setStartDate(LocalDate.of(2026, 1, 1));
        weekdayCalendarWithoutCalendarDate.setEndDate(LocalDate.of(2026, 12, 31));

        BusTrip trip = createTrip(
                "trip-106-default",
                line106,
                weekdayCalendarWithoutCalendarDate,
                "Testowy kierunek",
                0
        );

        createStopTime(trip, stopA, 1, "11:00:00", "11:00:00");
        createStopTime(trip, stopB, 2, "11:05:00", "11:05:00");

        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(get("/api/bus-lines/106/stops")
                        .param("direction", "default")
                        .param("date", ACTIVE_DATE.toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldUseOnlyCalendarDatesAndIgnoreWeekdayFlagsForDepartures() throws Exception {
        BusLine line107 = createLine("route-107", "107", "Linia 107");

        BusServiceCalendar weekdayCalendarWithoutCalendarDate = createCalendar("service-107-weekday-flags-only");
        weekdayCalendarWithoutCalendarDate.setMonday(true);
        weekdayCalendarWithoutCalendarDate.setTuesday(true);
        weekdayCalendarWithoutCalendarDate.setWednesday(true);
        weekdayCalendarWithoutCalendarDate.setThursday(true);
        weekdayCalendarWithoutCalendarDate.setFriday(true);
        weekdayCalendarWithoutCalendarDate.setStartDate(LocalDate.of(2026, 1, 1));
        weekdayCalendarWithoutCalendarDate.setEndDate(LocalDate.of(2026, 12, 31));

        BusTrip trip = createTrip(
                "trip-107-default",
                line107,
                weekdayCalendarWithoutCalendarDate,
                "Testowy kierunek",
                0
        );

        createStopTime(trip, stopA, 1, "12:00:00", "12:00:00");
        createStopTime(trip, stopB, 2, "12:05:00", "12:05:00");

        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(get("/api/bus-lines/107/stops/{stopId}/departures", stopA.getId())
                        .param("direction", "default")
                        .param("date", ACTIVE_DATE.toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRemoveServiceWhenCalendarDateHasExceptionTypeRemovedForLineStops() throws Exception {
        BusLine line108 = createLine("route-108", "108", "Linia 108");

        BusServiceCalendar removedCalendar = createCalendar("service-108-removed");
        createCalendarDate(removedCalendar, ACTIVE_DATE, GTFS_SERVICE_REMOVED);

        BusTrip trip = createTrip(
                "trip-108-default",
                line108,
                removedCalendar,
                "Usunięty kurs",
                0
        );

        createStopTime(trip, stopA, 1, "13:00:00", "13:00:00");
        createStopTime(trip, stopB, 2, "13:05:00", "13:05:00");

        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(get("/api/bus-lines/108/stops")
                        .param("direction", "default")
                        .param("date", ACTIVE_DATE.toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnDeparturesOnlyForServiceAddedByCalendarDate() throws Exception {
        BusLine line109 = createLine("route-109", "109", "Linia 109");

        BusServiceCalendar activeCalendar = createCalendar("service-109-active");
        createCalendarDate(activeCalendar, ACTIVE_DATE, GTFS_SERVICE_ADDED);

        BusServiceCalendar inactiveCalendar = createCalendar("service-109-inactive");

        BusTrip activeTrip = createTrip(
                "trip-109-active",
                line109,
                activeCalendar,
                "Aktywny kierunek",
                0
        );

        createStopTime(activeTrip, stopA, 1, "14:00:00", "14:00:00");

        BusTrip inactiveTrip = createTrip(
                "trip-109-inactive",
                line109,
                inactiveCalendar,
                "Nieaktywny kierunek",
                0
        );

        createStopTime(inactiveTrip, stopA, 1, "15:00:00", "15:00:00");

        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(get("/api/bus-lines/109/stops/{stopId}/departures", stopA.getId())
                        .param("direction", "default")
                        .param("date", ACTIVE_DATE.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lineNumber", is("109")))
                .andExpect(jsonPath("$.departures", hasSize(1)))
                .andExpect(jsonPath("$.departures[0].tripExternalId", is("trip-109-active")))
                .andExpect(jsonPath("$.departures[0].departureTime", is("14:00:00")));
    }

    @Test
    void shouldReturnStopsOnlyForServiceAddedByCalendarDate() throws Exception {
        BusLine line110 = createLine("route-110", "110", "Linia 110");

        BusServiceCalendar activeCalendar = createCalendar("service-110-active");
        createCalendarDate(activeCalendar, ACTIVE_DATE, GTFS_SERVICE_ADDED);

        BusServiceCalendar inactiveCalendar = createCalendar("service-110-inactive");

        BusTrip activeTrip = createTrip(
                "trip-110-active",
                line110,
                activeCalendar,
                "Aktywny kierunek",
                0
        );

        createStopTime(activeTrip, stopA, 1, "16:00:00", "16:00:00");
        createStopTime(activeTrip, stopB, 2, "16:05:00", "16:05:00");

        BusTrip inactiveTrip = createTrip(
                "trip-110-inactive",
                line110,
                inactiveCalendar,
                "Nieaktywny kierunek",
                0
        );

        createStopTime(inactiveTrip, stopA, 1, "17:00:00", "17:00:00");
        createStopTime(inactiveTrip, stopB, 2, "17:05:00", "17:05:00");
        createStopTime(inactiveTrip, stopC, 3, "17:10:00", "17:10:00");

        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(get("/api/bus-lines/110/stops")
                        .param("direction", "default")
                        .param("date", ACTIVE_DATE.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lineNumber", is("110")))
                .andExpect(jsonPath("$.representativeTripExternalId", is("trip-110-active")))
                .andExpect(jsonPath("$.stops", hasSize(2)))
                .andExpect(jsonPath("$.stops[0].name", is("Rondo Daszyńskiego")))
                .andExpect(jsonPath("$.stops[1].name", is("Muzeum Powstania Warszawskiego")));
    }

    @Test
    void shouldReturnAllBusLines() throws Exception {
        createLine("route-101", "101", "Linia 101");
        createLine("route-190", "190", "Linia 190");
        createLine("route-n01", "N01", "Linia N01");

        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(get("/api/bus-lines"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[0].lineNumber", is("101")))
                .andExpect(jsonPath("$[0].externalId", is("route-101")))
                .andExpect(jsonPath("$[0].name", is("Linia 101")))
                .andExpect(jsonPath("$[1].lineNumber", is("105")))
                .andExpect(jsonPath("$[1].externalId", is("route-105")))
                .andExpect(jsonPath("$[1].name", is("Linia 105")))
                .andExpect(jsonPath("$[2].lineNumber", is("190")))
                .andExpect(jsonPath("$[2].externalId", is("route-190")))
                .andExpect(jsonPath("$[2].name", is("Linia 190")))
                .andExpect(jsonPath("$[3].lineNumber", is("N01")))
                .andExpect(jsonPath("$[3].externalId", is("route-n01")))
                .andExpect(jsonPath("$[3].name", is("Linia N01")));
    }

    @Test
    void shouldReturnLinesEvenWhenTheyHaveNoTripsOrSchedule() throws Exception {
        createLine("route-777", "777", "Linia 777 bez rozkładu");

        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(get("/api/bus-lines"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].lineNumber", is("105")))
                .andExpect(jsonPath("$[1].lineNumber", is("777")))
                .andExpect(jsonPath("$[1].externalId", is("route-777")))
                .andExpect(jsonPath("$[1].name", is("Linia 777 bez rozkładu")));
    }

    @Test
    void shouldSortBusLinesNumericallyBeforeNightLines() throws Exception {
        createLine("route-9", "9", "Linia 9");
        createLine("route-100", "100", "Linia 100");
        createLine("route-12", "12", "Linia 12");
        createLine("route-n02", "N02", "Linia N02");
        createLine("route-n01", "N01", "Linia N01");

        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(get("/api/bus-lines"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(6)))
                .andExpect(jsonPath("$[0].lineNumber", is("9")))
                .andExpect(jsonPath("$[1].lineNumber", is("12")))
                .andExpect(jsonPath("$[2].lineNumber", is("100")))
                .andExpect(jsonPath("$[3].lineNumber", is("105")))
                .andExpect(jsonPath("$[4].lineNumber", is("N01")))
                .andExpect(jsonPath("$[5].lineNumber", is("N02")));
    }



    private BusLine createLine(String externalId, String lineNumber, String name) {
        BusLine line = new BusLine();
        line.setExternalId(externalId);
        line.setLineNumber(lineNumber);
        line.setName(name);
        line.setRouteType(3);

        entityManager.persist(line);

        return line;
    }

    private BusServiceCalendar createCalendar(String serviceId) {
        BusServiceCalendar calendar = new BusServiceCalendar();
        calendar.setServiceId(serviceId);

        calendar.setMonday(false);
        calendar.setTuesday(false);
        calendar.setWednesday(false);
        calendar.setThursday(false);
        calendar.setFriday(false);
        calendar.setSaturday(false);
        calendar.setSunday(false);

        calendar.setStartDate(null);
        calendar.setEndDate(null);

        entityManager.persist(calendar);

        return calendar;
    }

    private BusServiceCalendarDate createCalendarDate(
            BusServiceCalendar calendar,
            LocalDate date,
            Integer exceptionType
    ) {
        BusServiceCalendarDate calendarDate = new BusServiceCalendarDate();
        calendarDate.setServiceCalendar(calendar);
        calendarDate.setDate(date);
        calendarDate.setExceptionType(exceptionType);

        entityManager.persist(calendarDate);

        return calendarDate;
    }

    private BusStop createStop(
            String externalId,
            String stopCode,
            String name,
            String platformCode,
            String latitude,
            String longitude
    ) {
        BusStop stop = new BusStop();
        stop.setExternalId(externalId);
        stop.setStopCode(stopCode);
        stop.setName(name);
        stop.setPlatformCode(platformCode);
        stop.setLatitude(new BigDecimal(latitude));
        stop.setLongitude(new BigDecimal(longitude));

        entityManager.persist(stop);

        return stop;
    }

    private BusTrip createTrip(
            String externalId,
            BusLine line,
            BusServiceCalendar calendar,
            String headsign,
            Integer directionId
    ) {
        BusTrip trip = new BusTrip();
        trip.setExternalId(externalId);
        trip.setLine(line);
        trip.setServiceCalendar(calendar);
        trip.setHeadsign(headsign);
        trip.setDirectionId(directionId);

        entityManager.persist(trip);

        return trip;
    }

    private BusStopTime createStopTime(
            BusTrip trip,
            BusStop stop,
            Integer stopSequence,
            String arrivalTime,
            String departureTime
    ) {
        BusStopTime stopTime = new BusStopTime();
        stopTime.setTrip(trip);
        stopTime.setStop(stop);
        stopTime.setStopSequence(stopSequence);
        stopTime.setArrivalTime(arrivalTime);
        stopTime.setDepartureTime(departureTime);
        stopTime.setArrivalSeconds(toSeconds(arrivalTime));
        stopTime.setDepartureSeconds(toSeconds(departureTime));

        entityManager.persist(stopTime);

        return stopTime;
    }

    private int toSeconds(String time) {
        String[] parts = time.split(":");

        int hours = Integer.parseInt(parts[0]);
        int minutes = Integer.parseInt(parts[1]);
        int seconds = Integer.parseInt(parts[2]);

        return hours * 3600 + minutes * 60 + seconds;
    }
}