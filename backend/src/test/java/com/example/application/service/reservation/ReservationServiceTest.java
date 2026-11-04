package com.example.application.service.reservation;

import com.example.application.dto.*;
import com.example.application.entity.forCustomer.CustomerEntity;
import com.example.application.entity.forProduct.ProductEntity;
import com.example.application.entity.forReservation.*;
import com.example.application.repository.reservation.ReservationRepository;
import com.example.application.service.customer.CustomerService;
import com.example.application.service.product.ProductService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {
    @Mock ReservationRepository repository;
    @Mock ReservationItemService items;
    @Mock ProductService products;
    @Mock CustomerService customers;
    @InjectMocks ReservationService service;

    // Object Mother: each call creates independent objects.
    private ProductEntity product(int stock, int reserved) {
        ProductEntity p = new ProductEntity();
        p.setSku("TEST-001"); p.setListPrice(new BigDecimal("1000"));
        p.setQuantityOnHand(stock); p.setQuantityReserved(reserved); return p;
    }
    private ListProductsAndCostumerDto request(int count) {
        DtoProduct line = new DtoProduct(); line.setSku("TEST-001"); line.setCount(count);
        ListProductsAndCostumerDto d = new ListProductsAndCostumerDto();
        d.setCustomer(new CustomerEntity()); d.setStartAt(LocalDate.of(2026,11,4));
        d.setEndAt(LocalDate.of(2026,11,7)); d.setSkuAndCount(List.of(line)); return d;
    }
    private ReservationEntity reservation(ReservationStatus status) {
        ReservationEntity r = new ReservationEntity();r.setReservationNum("R-001");r.setStatus(status);return r;
    }
    private void arrangeRequest(ListProductsAndCostumerDto d, ProductEntity p) {
        when(customers.saveDB(d.getCustomer())).thenReturn(d.getCustomer());
        when(products.findBySku("TEST-001")).thenReturn(p);
    }
    @Test void createsActiveReservation() {
        // Arrange
        CustomerEntity c = new CustomerEntity(); LocalDate start=LocalDate.of(2026,11,4);
        // Act
        ReservationEntity r=service.getNewReservationAndSave(c,start,start.plusDays(3));
        // Assert
        assertSame(c,r.getCustomer()); assertEquals(start,r.getStartAt());
        assertEquals(start.plusDays(3),r.getEndAt());assertEquals(ReservationStatus.ACTUAL,r.getStatus());
        assertNotNull(r.getReservationNum());assertFalse(r.getReservationNum().isBlank());verify(repository).save(r);
    }

    @Test void retriesNumberCollision() {
        when(repository.existsByReservationNum(anyString())).thenReturn(true,false);
        var c=new CustomerEntity();
        var r=service.getNewReservationAndSave(c,LocalDate.of(2026,11,4),LocalDate.of(2026,11,7));
        assertNotNull(r.getReservationNum());verify(repository,times(2)).existsByReservationNum(anyString());
    }

    @Test void reservesAvailableQuantity() {
        var d=request(2);var p=product(5,1);arrangeRequest(d,p);
        var result=service.madeReservation(d);
        assertTrue(result.isPresent());assertEquals(3,p.getQuantityReserved());
        var captor=ArgumentCaptor.forClass(ReservationItemEntity.class);verify(items).save(captor.capture());
        assertEquals(2,captor.getValue().getQuantity());assertSame(p,captor.getValue().getProduct());
        assertEquals(new BigDecimal("1000"),captor.getValue().getUnitPrice());
    }

    @Test void reservesExactlyAvailableQuantity() {
        var d=request(3);var p=product(5,2);arrangeRequest(d,p);
        assertTrue(service.madeReservation(d).isPresent());assertEquals(5,p.getQuantityReserved());
    }

    @Test void rejectsQuantityAboveStock() {
        var d=request(6);var p=product(5,0);arrangeRequest(d,p);
        assertThrows(IllegalArgumentException.class,()->service.madeReservation(d));
        assertEquals(0,p.getQuantityReserved());verify(items,never()).save(any());
    }

    @Test void rejectsQuantityAboveAvailableStock() {
        var d=request(3);var p=product(5,4);arrangeRequest(d,p);
        assertTrue(service.madeReservation(d).isEmpty());assertEquals(4,p.getQuantityReserved());
        verify(repository).delete(any(ReservationEntity.class));verify(items).deleteAllByReservation(any());
    }

    @Test void returnsExistingReservation() {
        var r=reservation(ReservationStatus.ACTUAL);when(repository.findByReservationNum("R-001")).thenReturn(Optional.of(r));
        assertSame(r,service.findByReservationNum("R-001"));
    }

    @Test void rejectsUnknownReservation() {
        assertThrows(IllegalArgumentException.class,()->service.findByReservationNum("UNKNOWN"));
    }

    @Test void cancelsAndReleasesStock() {
        var r=reservation(ReservationStatus.ACTUAL);var p=product(5,2);
        var line=new ReservationItemEntity();line.setProduct(p);line.setQuantity(2);
        when(repository.findByReservationNum("R-001")).thenReturn(Optional.of(r));
        when(items.findAllByReservationNum("R-001")).thenReturn(List.of(line));
        var result=service.setStatusReservation("R-001",ReservationStatus.INVALID);
        assertSame(r,result);assertEquals(0,p.getQuantityReserved());assertEquals(ReservationStatus.INVALID,r.getStatus());verify(repository).save(r);
    }

    @Test void rejectsCancellationOfUnknownReservation() {
        assertThrows(IllegalArgumentException.class,()->service.setStatusReservation("UNKNOWN",ReservationStatus.INVALID));
        verifyNoInteractions(items);
    }

    @Test void findsReservationsByCustomerPhone() {
        var c=new CustomerEntity();var expected=List.of(reservation(ReservationStatus.ACTUAL));
        when(customers.findByPhone("+79001234567")).thenReturn(c);when(repository.findAllByCustomer(c)).thenReturn(expected);
        assertSame(expected,service.findByPhone("+79001234567"));
    }

    @Test void delegatesExpirationUpdate() {
        var day=LocalDate.of(2026,11,4);
        service.checkAllReservationAndReplaceStatus(day,ReservationStatus.ACTUAL,ReservationStatus.INVALID);
        verify(repository).replaceReservationStatusIfInvalid(day,ReservationStatus.ACTUAL,ReservationStatus.INVALID);
    }
}
