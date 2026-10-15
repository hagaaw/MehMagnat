package com.example.application.repository.reservation;

import com.example.application.entity.forReservation.ReservationEntity;
import com.example.application.entity.forReservation.ReservationItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

public interface ReservationItemRepository extends JpaRepository<ReservationItemEntity, Long> {
    @Query("""
    SELECT SUM(ri.quantity) FROM ReservationItemEntity ri
    WHERE ri.product.sku = :sku
      AND ri.reservation.startAt <= :newEnd
      AND ri.reservation.endAt   >= :newStart
      AND ri.reservation.status IN ('ACTUAL')
""")
    Integer isItemBusy(
            @Param("sku") String sku,
            @Param("newStart") LocalDate newStart,
            @Param("newEnd") LocalDate newEnd
    );


    @Query("""
    SELECT SUM(ri.quantity) FROM ReservationItemEntity ri
    WHERE ri.product.sku = :sku 
    AND ri.reservation.status IN ('ACTUAL')
""")
    Integer countBusy(String sku);

    List<ReservationItemEntity> findAllByReservation(ReservationEntity reservation);


    void deleteAllByReservation(ReservationEntity reservation);
}
