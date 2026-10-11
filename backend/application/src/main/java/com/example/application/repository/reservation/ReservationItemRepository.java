package com.example.application.repository.reservation;

import com.example.application.entity.forReservation.ReservationItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationItemRepository extends JpaRepository<ReservationItemEntity, Long> {
}
