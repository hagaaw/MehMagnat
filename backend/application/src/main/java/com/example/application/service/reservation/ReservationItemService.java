package com.example.application.service.reservation;

import com.example.application.entity.forReservation.ReservationItemEntity;
import com.example.application.repository.reservation.ReservationItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@Transactional
public class ReservationItemService {
    private final ReservationItemRepository reservationItemRepository;

    public ReservationItemService(ReservationItemRepository reservationItemRepository) {
        this.reservationItemRepository = reservationItemRepository;
    }

    public Integer checkOnValid(String sku, LocalDate startAt, LocalDate endAt) {
        return reservationItemRepository.isItemBusy(sku, startAt, endAt);
    }

    public void save(ReservationItemEntity reservationItemEntity) {
        reservationItemRepository.save(reservationItemEntity);
    }
}
