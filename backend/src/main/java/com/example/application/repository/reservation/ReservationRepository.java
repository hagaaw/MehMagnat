package com.example.application.repository.reservation;

import com.example.application.entity.forCustomer.CustomerEntity;
import com.example.application.entity.forReservation.ReservationEntity;
import com.example.application.entity.forReservation.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<ReservationEntity, Long> {
    boolean existsByReservationNum(String reservationNum);

    @Modifying
    @Transactional
    @Query("""
    UPDATE ReservationEntity r
    SET r.status = :newStatus
    WHERE r.endAt <= :now AND r.status = :oldStatus
    """)
    void replaceReservationStatusIfInvalid(LocalDate now, ReservationStatus oldStatus,ReservationStatus newStatus);

    Optional<ReservationEntity> findByReservationNum(String reservationNum);


    List<ReservationEntity> findAllByCustomer(CustomerEntity customer);
}
