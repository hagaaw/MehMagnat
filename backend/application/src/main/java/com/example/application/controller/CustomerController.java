package com.example.application.controller;


import com.example.application.dto.ListProductsAndCostumerDto;
import com.example.application.entity.forProduct.ProductEntity;
import com.example.application.entity.forReservation.ReservationEntity;
import com.example.application.entity.forReservation.ReservationItemEntity;
import com.example.application.entity.forReservation.ReservationStatus;
import com.example.application.service.customer.CustomerService;
import com.example.application.service.product.ProductService;
import com.example.application.service.reservation.ReservationItemService;
import com.example.application.service.reservation.ReservationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.swing.text.html.HTML;

@RestController
@RequestMapping("/api/customer")
@Transactional
public class CustomerController {
    private final CustomerService customerService;
    private final ReservationService reservationService;
    private final ReservationItemService reservationItemService;
    private final ProductService productService;
    private final Logger logger = LoggerFactory.getLogger(CustomerController.class);

    public CustomerController(CustomerService customerService, ReservationService reservationService, ReservationItemService reservationItemService, ProductService productService) {
        this.customerService = customerService;
        this.reservationService = reservationService;
        this.reservationItemService = reservationItemService;
        this.productService = productService;
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
