package com.rozkladjazdy.jazdaz.database.repositories;

import com.rozkladjazdy.jazdaz.database.entities.BusStopEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BusStopRepository extends JpaRepository<BusStopEntity, Long> {

    Optional<BusStopEntity> findByExternalId(String externalId);

    boolean existsByExternalId(String externalId);
}