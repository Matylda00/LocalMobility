package com.rozkladjazdy.jazdaz.database.repositories;


import com.rozkladjazdy.jazdaz.database.entities.BusServiceCalendarDate;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
@Repository
public interface BusServiceCalendarDateRepository extends JpaRepository<BusServiceCalendarDate, Long> {

    @EntityGraph(attributePaths = "serviceCalendar")
    List<BusServiceCalendarDate> findByDate(LocalDate date);
}