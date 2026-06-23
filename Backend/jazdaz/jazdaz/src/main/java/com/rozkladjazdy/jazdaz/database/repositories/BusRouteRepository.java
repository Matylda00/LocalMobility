package com.rozkladjazdy.jazdaz.database.repositories;

import com.rozkladjazdy.jazdaz.database.entities.BusRouteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BusRouteRepository extends JpaRepository<BusRouteEntity, Long> {

    Optional<BusRouteEntity> findByExternalId(String externalId);
}