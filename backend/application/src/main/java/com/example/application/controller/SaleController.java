package com.example.application.controller;


import com.example.application.entity.forEmployee.EmployeeEntity;
import com.example.application.entity.forProduct.ProductEntity;
import com.example.application.entity.forReservation.ReservationEntity;
import com.example.application.entity.forReservation.ReservationItemEntity;
import com.example.application.entity.forSale.SaleEntity;
import com.example.application.service.reservation.ReservationItemService;
import com.example.application.service.reservation.ReservationService;
import com.example.application.service.sale.SaleService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/sale")
public class SaleController {
    private final ReservationService reservationService;
    private final ReservationItemService reservationItemService;
    private final SaleService saleService;

    public SaleController(ReservationService reservationService, ReservationItemService reservationItemService, SaleService saleService) {
        this.reservationService = reservationService;
        this.reservationItemService = reservationItemService;
        this.saleService = saleService;
    }

    @PostMapping("/add")
    public ResponseEntity<?> sale(@RequestParam String reservationNum
                                                  ){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String phoneEmployee = auth.getName();


        SaleEntity sale = saleService.sale(reservationNum, phoneEmployee);

        return ResponseEntity.ok(sale);
    }
}
