package com.example.application.controller;

import com.example.application.dto.ListProductsAndCostumerDto;
import com.example.application.service.customer.CustomerService;
import com.example.application.service.reservation.ReservationService;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/reservation")
@Transactional
public class ReservationController {
    private final CustomerService customerService;
    private final ReservationService reservationService;

    public ReservationController(CustomerService customerService, ReservationService reservationService) {
        this.customerService = customerService;
        this.reservationService = reservationService;
    }

    @PostMapping("/made_reservation")
    public ResponseEntity<String> madeReservation(
            @RequestBody ListProductsAndCostumerDto listProductsAndCostumerDto
    ){

        String reservationNum = reservationService.madeReservation(listProductsAndCostumerDto).orElseThrow(() -> new IllegalArgumentException("Бронь не найдена"));
        return ResponseEntity.ok(reservationNum);

    }

}
