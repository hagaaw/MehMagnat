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
class ReservationValidationTest {
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
    @Test void rejectsZeroQuantity() {
        var d=request(0);var p=product(5,0);lenient().when(customers.saveDB(d.getCustomer())).thenReturn(d.getCustomer());
        lenient().when(products.findBySku("TEST-001")).thenReturn(p);
        assertThrows(IllegalArgumentException.class,()->service.madeReservation(d));
    }

    @Test void repeatedCancellationDoesNotReleaseStockAgain() {
        var r=reservation(ReservationStatus.INVALID);var p=product(5,0);
        var line=new ReservationItemEntity();line.setProduct(p);line.setQuantity(2);
        when(repository.findByReservationNum("R-001")).thenReturn(Optional.of(r));
        lenient().when(items.findAllByReservationNum("R-001")).thenReturn(List.of(line));
        service.setStatusReservation("R-001",ReservationStatus.INVALID);
        assertEquals(0,p.getQuantityReserved(),"A repeated cancellation must not make stock negative");
    }
}
