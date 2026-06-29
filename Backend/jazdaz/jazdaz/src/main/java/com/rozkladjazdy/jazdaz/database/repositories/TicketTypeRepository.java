package com.rozkladjazdy.jazdaz.database.repositories;

import com.rozkladjazdy.jazdaz.database.entities.TicketType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TicketTypeRepository extends JpaRepository<TicketType, Long> {
}