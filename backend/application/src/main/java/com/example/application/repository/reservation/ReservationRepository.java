package com.example.application.repository.reservation;

import com.example.application.entity.forReservation.ReservationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationRepository extends JpaRepository<ReservationEntity, Long> {
    boolean existsByReservationNum(String reservationNum);
}
