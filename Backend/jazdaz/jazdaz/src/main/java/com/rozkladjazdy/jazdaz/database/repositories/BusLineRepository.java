package com.rozkladjazdy.jazdaz.database.repositories;


import com.rozkladjazdy.jazdaz.database.entities.BusLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
@Repository
public interface BusLineRepository extends JpaRepository<BusLine, Long> {

    Optional<BusLine> findByLineNumber(String lineNumber);

    boolean existsByLineNumber(String lineNumber);

    List<BusLine> findAll();
}