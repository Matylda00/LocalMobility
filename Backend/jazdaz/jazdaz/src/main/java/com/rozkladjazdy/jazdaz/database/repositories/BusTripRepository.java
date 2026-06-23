package com.rozkladjazdy.jazdaz.database.repositories;


import com.rozkladjazdy.jazdaz.database.entities.BusTrip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


import java.util.List;

@Repository
public interface BusTripRepository extends JpaRepository<BusTrip, Long> {

    List<BusTrip> findByLineLineNumberAndDirectionId(String lineNumber, Integer directionId);
    List<BusTrip> findByLineLineNumberAndDirectionIdAndServiceCalendarIdIn(
            String lineNumber,
            Integer directionId,
            List<Long> serviceCalendarIds
    );
}