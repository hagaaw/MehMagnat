package com.example.application.controller;

import com.example.application.dto.ListProductsAndCostumerDto;
import com.example.application.service.customer.CustomerService;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/reservation")
@Transactional
public class ReservationController {
    private final CustomerService customerService;

    public ReservationController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping("/made_reservation")
    public ResponseEntity<?> madeReservation(
            @RequestBody ListProductsAndCostumerDto listProductsAndCostumerDto
    ){

        Boolean isCreate = customerService.madeReservation(listProductsAndCostumerDto);
        if (isCreate){
            return ResponseEntity.ok(isCreate);
        }
        return ResponseEntity.status(400).body(isCreate);

    }
}
