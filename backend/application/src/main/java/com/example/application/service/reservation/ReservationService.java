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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private final Logger logger = LoggerFactory.getLogger(ReservationService.class);

    public ReservationService(ReservationRepository reservationRepository, ReservationItemService reservationItemService, ProductService productService, CustomerService customerService) {
        this.reservationRepository = reservationRepository;
        this.reservationItemService = reservationItemService;
        this.productService = productService;
        this.customerService = customerService;
    }

    public ReservationEntity getNewReservationAndSave(CustomerEntity customer, LocalDate startAt, LocalDate endAt) {

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
            reservationNum = generateRandomString(len);
        } while (reservationRepository.existsByReservationNum(reservationNum));
        return reservationNum;
    }



    public Optional<String> madeReservation(ListProductsAndCostumerDto listProductsAndCostumerDto) {
        var customer = customerService.saveDB(listProductsAndCostumerDto.getCustomer());

        ReservationEntity reservation = getNewReservationAndSave(customer,
                listProductsAndCostumerDto.getStartAt(),
                listProductsAndCostumerDto.getEndAt());

        for(var dtoProduct: listProductsAndCostumerDto.getSkuAndCount()){
            ProductEntity product = productService.findBySku(dtoProduct.getSku());
            if (productService.findBySku(dtoProduct.getSku()).getQuantityReserved()
                    < dtoProduct.getCount()){
                throw new IllegalArgumentException();
            }
            Integer countBusy = reservationItemService.checkBusyInDate(dtoProduct.getSku(),
                    listProductsAndCostumerDto.getStartAt(),
                    listProductsAndCostumerDto.getEndAt());

            if ( countBusy == null){
                countBusy = 0;
            }

            int countFreeProducts = product.getQuantityReserved() - reservationItemService.checkBusy(dtoProduct.getSku());

            if (-countBusy + countFreeProducts >= dtoProduct.getCount()){

                ReservationItemEntity reservationItemEntity = new ReservationItemEntity(
                        dtoProduct.getCount(),
                        product.getListPrice(),
                        0d,
                        reservation,
                        product
                );
                reservationItemService.save(reservationItemEntity);
            }else {
                logger.info("бляяяя");
                reservationRepository.delete(reservation);
                reservationItemService.deleteAllByReservation(reservation);
                return Optional.empty();
            }
        }

        reservation.setStatus(ReservationStatus.ACTUAL);
        return Optional.of(reservation.getReservationNum());


    }

    public void checkAllReservationAndReplaceStatus(LocalDate now, ReservationStatus oldStatus,ReservationStatus newStatus) {
        reservationRepository.replaceReservationStatusIfInvalid(now,oldStatus ,newStatus);
    }

    public ReservationEntity findByReservationNum(String reservationNum) {
        return reservationRepository.findByReservationNum(reservationNum).orElseThrow(() -> new IllegalArgumentException("Такой брони нет"));
    }
}
