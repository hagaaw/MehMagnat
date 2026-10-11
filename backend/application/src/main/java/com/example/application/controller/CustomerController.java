package com.example.application.controller;


import com.example.application.dto.ListProductsAndCostumerDto;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customer")
@Transactional
public class CustomerController {
    @PostMapping("/made_reservation")
    public ResponseEntity<?> madeReservation(
            @RequestBody ListProductsAndCostumerDto listProductsAndCostumerDto
            ){

    }
}
