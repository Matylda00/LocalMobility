package com.rozkladjazdy.jazdaz.database.repositories;

import com.rozkladjazdy.jazdaz.database.entities.BusRouteEntity;
import com.rozkladjazdy.jazdaz.database.entities.BusRouteStopEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BusRouteStopRepository extends JpaRepository<BusRouteStopEntity, Long> {

    List<BusRouteStopEntity> findByRouteOrderByStopSequence(BusRouteEntity route);
}