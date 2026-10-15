package com.example.application.service.reservation;

import com.example.application.entity.forReservation.ReservationEntity;
import com.example.application.entity.forReservation.ReservationItemEntity;
import com.example.application.repository.reservation.ReservationItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class ReservationItemService {
    private final ReservationItemRepository reservationItemRepository;

    public ReservationItemService(ReservationItemRepository reservationItemRepository) {
        this.reservationItemRepository = reservationItemRepository;
    }

    public Integer checkBusyInDate(String sku, LocalDate startAt, LocalDate endAt) {
        return reservationItemRepository.isItemBusy(sku, startAt, endAt);
    }

    public void save(ReservationItemEntity reservationItemEntity) {
        reservationItemRepository.save(reservationItemEntity);
    }

    public List<ReservationItemEntity> findAllByReservationNum(ReservationEntity reservation) {
        return reservationItemRepository.findAllByReservation(reservation);
    }

    public void deleteAllByReservation(ReservationEntity reservation) {
        reservationItemRepository.deleteAllByReservation(reservation);
    }

    public int checkBusy(String sku) {
        var count = reservationItemRepository.countBusy(sku);
        if (count == null) return 0;
        return count;
    }
}
