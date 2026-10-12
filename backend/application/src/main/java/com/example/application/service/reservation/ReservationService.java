package com.example.application.service.reservation;


import com.example.application.dto.ListProductsAndCostumerDto;
import com.example.application.entity.forCustomer.CustomerEntity;
import com.example.application.entity.forProduct.ProductEntity;
import com.example.application.entity.forReservation.ReservationEntity;
import com.example.application.entity.forReservation.ReservationItemEntity;
import com.example.application.entity.forReservation.ReservationStatus;
import com.example.application.repository.reservation.ReservationRepository;
import com.example.application.service.customer.CustomerService;
import com.example.application.service.product.ProductService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static com.example.application.config.AppConfig.*;

@Service
@Transactional
public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final ReservationItemService reservationItemService;
    private final ProductService productService;
    private final CustomerService customerService;

    public ReservationService(ReservationRepository reservationRepository, ReservationItemService reservationItemService, ProductService productService, CustomerService customerService) {
        this.reservationRepository = reservationRepository;
        this.reservationItemService = reservationItemService;
        this.productService = productService;
        this.customerService = customerService;
    }

    public ReservationEntity getNewReservation(CustomerEntity customer, LocalDate startAt, LocalDate endAt) {

        var reservation = new ReservationEntity(
                generatedUniqueNum(),
                customer,
                ReservationStatus.ACTUAL,
                startAt,
                endAt,
                LocalDateTime.now(),
                "новая бронь"
        );

        reservationRepository.save(reservation);



        return reservation;



    }

    private String generatedUniqueNum() {
        String reservationNum;
        do {
            reservationNum = generateRandomString();
        } while (reservationRepository.existsByReservationNum(reservationNum));
        return reservationNum;
    }

    private String generateRandomString() {
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(CHARS.charAt(random.nextInt(CHARS.length())));
        }
        return sb.toString();
    }

    public Boolean madeReservation(ListProductsAndCostumerDto listProductsAndCostumerDto) {
        int count = 0;
        var customer = customerService.saveDB(listProductsAndCostumerDto.getCustomer());

        ReservationEntity reservation = getNewReservation(customer,
                listProductsAndCostumerDto.getStartAt(),
                listProductsAndCostumerDto.getEndAt());

        for(var dtoProduct: listProductsAndCostumerDto.getSkuAndCount()){
            ProductEntity product = productService.findBySku(dtoProduct.getSku());
            if (product == null){
                continue;
            }
            if (productService.findBySku(dtoProduct.getSku()).getQuantityReserved()
                    < dtoProduct.getCount()){
                continue;
            }
            Integer countBusy = reservationItemService.checkOnValid(dtoProduct.getSku(),
                    listProductsAndCostumerDto.getStartAt(),
                    listProductsAndCostumerDto.getEndAt());

            if ( countBusy == null){
                countBusy = 0;
            }


            if (-countBusy + product.getQuantityReserved() >= dtoProduct.getCount()){

                ReservationItemEntity reservationItemEntity = new ReservationItemEntity(
                        dtoProduct.getCount(),
                        product.getListPrice(),
                        0d,
                        reservation,
                        product
                );
                reservationItemService.save(reservationItemEntity);
                count++;
            }
        }
        if (count != 0){
            reservation.setStatus(ReservationStatus.ACTUAL);
            return true;
        }
        return false;
    }

    public void checkAllReservationAndReplaceStatus(LocalDate now, ReservationStatus oldStatus,ReservationStatus newStatus) {
        reservationRepository.replaceReservationStatusIfInvalid(now,oldStatus ,newStatus);
    }
}
