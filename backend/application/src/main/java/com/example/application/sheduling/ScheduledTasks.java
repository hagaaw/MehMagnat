package com.example.application.sheduling;

import com.example.application.entity.forReservation.ReservationStatus;
import com.example.application.service.reservation.ReservationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;


@Component
public class ScheduledTasks {
    private static final Logger logger = LoggerFactory.getLogger(ScheduledTasks.class);
    private final ReservationService reservationService;

    public ScheduledTasks(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @Scheduled(cron = "@midnight")
    public void checkReservationAndReplaceStatus(){
        reservationService.checkAllReservationAndReplaceStatus(LocalDate.now(), ReservationStatus.ACTUAL,ReservationStatus.INVALID);
    }
}

