package com.rozkladjazdy.jazdaz.database.repositories;

import com.rozkladjazdy.jazdaz.database.entities.BusLineEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BusLineRepository extends JpaRepository<BusLineEntity, Long> {

    Optional<BusLineEntity> findByLineNumber(String lineNumber);

    boolean existsByLineNumber(String lineNumber);

    Optional<BusLineEntity> findByExternalId(String routeId);
}