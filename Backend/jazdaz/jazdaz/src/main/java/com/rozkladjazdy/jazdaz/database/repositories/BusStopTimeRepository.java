package com.rozkladjazdy.jazdaz.database.repositories;


import com.rozkladjazdy.jazdaz.database.entities.BusStopTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface BusStopTimeRepository extends JpaRepository<BusStopTime, Long> {

    long countByTripId(Long tripId);


    List<BusStopTime> findByTripIdOrderByStopSequenceAsc(Long tripId);
    List<BusStopTime> findByTripLineLineNumberAndTripDirectionIdAndTripServiceCalendarIdInAndStopIdOrderByDepartureSecondsAsc(
            String lineNumber,
            Integer directionId,
            List<Long> serviceCalendarIds,
            Long stopId
    );
}