package com.example.application.service.reservation;

import com.example.application.entity.forReservation.ReservationEntity;
import com.example.application.entity.forReservation.ReservationItemEntity;
import com.example.application.repository.reservation.ReservationItemRepository;
import com.example.application.repository.reservation.ReservationRepository;
import com.example.application.service.product.ProductService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class ReservationItemService {
    private final ReservationItemRepository reservationItemRepository;
    private final ReservationRepository reservationRepository;
    private final ProductService productService;


    public ReservationItemService(ReservationItemRepository reservationItemRepository, ReservationRepository reservationRepository, ProductService productService) {
        this.reservationItemRepository = reservationItemRepository;
        this.reservationRepository = reservationRepository;
        this.productService = productService;
    }

    public Integer checkBusyInDate(String sku, LocalDate startAt, LocalDate endAt) {
        return reservationItemRepository.isItemBusy(sku, startAt, endAt);
    }

    public void save(ReservationItemEntity reservationItemEntity) {
        reservationItemRepository.save(reservationItemEntity);
    }

    public List<ReservationItemEntity> findAllByReservationNum(String reservationNum) {
        ReservationEntity reservation = reservationRepository.findByReservationNum(reservationNum).orElseThrow(()-> new IllegalArgumentException("not found"));
        return reservationItemRepository.findAllByReservation(reservation);
    }

    public void deleteAllByReservation(ReservationEntity reservation) {
        reservationItemRepository.deleteAllByReservation(reservation);
    }


}
