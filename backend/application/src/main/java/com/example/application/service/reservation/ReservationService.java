package com.example.application.service.reservation;


import com.example.application.entity.forCustomer.CustomerEntity;
import com.example.application.entity.forReservation.ReservationEntity;
import com.example.application.entity.forReservation.ReservationStatus;
import com.example.application.repository.reservation.ReservationRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static com.example.application.config.AppConfig.*;

@Service
public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final ReservationItemService reservationItemService;

    public ReservationService(ReservationRepository reservationRepository, ReservationItemService reservationItemService) {
        this.reservationRepository = reservationRepository;
        this.reservationItemService = reservationItemService;
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
}
