package com.example.application.controller;


import com.example.application.entity.forReservation.ReservationEntity;
import com.example.application.entity.forReservation.ReservationItemEntity;
import com.example.application.service.reservation.ReservationItemService;
import com.example.application.service.reservation.ReservationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reservation_item")
public class ReservationItemController {
    private final ReservationItemService reservationItemService;
    private final ReservationService reservationService;

    public ReservationItemController(ReservationItemService reservationItemService, ReservationService reservationService) {
        this.reservationItemService = reservationItemService;
        this.reservationService = reservationService;
    }

    @GetMapping
    public ResponseEntity<List<ReservationItemEntity>> findAllReservationByReservationNum(String reservationNum){
        ReservationEntity reservation = reservationService.findByReservationNum(reservationNum);

        return ResponseEntity.ok(reservationItemService.findAllByReservationNum(reservation));
    }
}
