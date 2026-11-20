package com.example.application.controller;

import com.example.application.entity.forReservation.*;
import com.example.application.repository.reservation.*;
import com.example.application.service.product.ProductService;
import com.example.application.service.reservation.*;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReservationItemsTest {
    final ReservationItemRepository items = mock(ReservationItemRepository.class);
    final ReservationRepository bookings = mock(ReservationRepository.class);
    final ReservationItemService service = new ReservationItemService(items, bookings, mock(ProductService.class));
    final ReservationItemController controller = new ReservationItemController(service,mock(ReservationService.class));

    @Test void findsOnlyItemsOfTheRequestedBooking() {
        LocalDate start = LocalDate.of(2026,11,10);
        var booking = new ReservationEntity("R1",null,ReservationStatus.ACTUAL,start,start.plusDays(2),start.atStartOfDay(),"");
        var item = new ReservationItemEntity();
        item.setReservation(booking);
        when(bookings.findByReservationNum("R1")).thenReturn(Optional.of(booking));
        when(items.findAllByReservation(booking)).thenReturn(List.of(item));
        assertEquals(List.of(item),controller.findAllReservationByReservationNum("R1").getBody());
        assertEquals("R1",item.getReservation().getReservationNum());
    }

    @Test void missingBookingStopsItemLookup() {
        assertThrows(IllegalArgumentException.class, () -> service.findAllByReservationNum("absent"));
        verifyNoInteractions(items);
    }

    @Test void checksAvailabilityForRequestedDates() {
        var start = LocalDate.of(2026,11,10);
        var end = start.plusDays(2);
        when(items.isItemBusy("M1",start,end)).thenReturn(2);
        assertEquals(2,service.checkBusyInDate("M1",start,end));
    }
}
