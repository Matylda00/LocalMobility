package com.rozkladjazdy.jazdaz.database.repositories;


import com.rozkladjazdy.jazdaz.database.entities.BusStop;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusStopRepository extends JpaRepository<BusStop, Long> {
}