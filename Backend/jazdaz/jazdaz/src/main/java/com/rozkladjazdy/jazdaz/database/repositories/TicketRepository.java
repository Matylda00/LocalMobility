package com.rozkladjazdy.jazdaz.database.repositories;

import com.rozkladjazdy.jazdaz.database.entities.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    List<Ticket> findByUser_EmailOrderByPurchaseDateDesc(String email);

    Optional<Ticket> findByUuid(String uuid);

    Optional<Ticket> findByQrCode(String qrCode);

    boolean existsByUuid(String uuid);

    boolean existsByQrCode(String qrCode);
}