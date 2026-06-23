package com.rozkladjazdy.jazdaz.database.repositories;


import com.rozkladjazdy.jazdaz.database.entities.BusServiceCalendar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BusServiceCalendarRepository extends JpaRepository<BusServiceCalendar, Long> {
}