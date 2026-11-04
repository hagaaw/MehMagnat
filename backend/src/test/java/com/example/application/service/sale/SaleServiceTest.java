package com.example.application.service.sale;

import com.example.application.entity.forCustomer.CustomerEntity;
import com.example.application.entity.forEmployee.EmployeeEntity;
import com.example.application.entity.forProduct.ProductEntity;
import com.example.application.entity.forReservation.ReservationEntity;
import com.example.application.entity.forReservation.ReservationItemEntity;
import com.example.application.entity.forReservation.ReservationStatus;
import com.example.application.entity.forSale.SaleEntity;
import com.example.application.entity.forSale.SaleItemEntity;
import com.example.application.entity.forSale.SaleStatus;
import com.example.application.repository.sale.SaleItemRepository;
import com.example.application.repository.sale.SaleRepository;
import com.example.application.service.customer.CustomerService;
import com.example.application.service.employee.EmployeeService;
import com.example.application.service.product.ProductService;
import com.example.application.service.reservation.ReservationItemService;
import com.example.application.service.reservation.ReservationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SaleServiceTest {
    private static final String NUMBER = "R-SALE-001";
    private static final String PHONE = "+79001234567";

    @Mock ReservationService reservations;
    @Mock ReservationItemService reservationItems;
    @Mock SaleRepository sales;
    @Mock EmployeeService employees;
    @Mock CustomerService customers;
    @Mock SaleItemRepository saleItemRepository;
    @Mock SaleItemService saleItems;
    @Mock ProductService products;
    @InjectMocks SaleService service;

    private ReservationEntity reservation(ReservationStatus status) {
        var reservation = new ReservationEntity();
        reservation.setReservationNum(NUMBER);
        reservation.setStatus(status);
        reservation.setCustomer(new CustomerEntity());
        return reservation;
    }

    private ReservationItemEntity line(int quantity, String price, double discount) {
        var product = new ProductEntity();
        product.setQuantityOnHand(10);
        product.setQuantityReserved(quantity);
        return new ReservationItemEntity(quantity, new BigDecimal(price), discount,
                null, product);
    }

    private EmployeeEntity arrange(ReservationEntity reservation,
                                   List<ReservationItemEntity> lines) {
        var employee = new EmployeeEntity();
        when(reservations.findByReservationNum(NUMBER)).thenReturn(reservation);
        when(employees.findByPhone(PHONE)).thenReturn(employee);
        when(reservationItems.findAllByReservationNum(NUMBER)).thenReturn(lines);
        return employee;
    }

    private void assertMoney(String expected, BigDecimal actual) {
        assertNotNull(actual);
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }

    @Test
    void completesSaleForOneItem() {
        // Arrange
        var reservation = reservation(ReservationStatus.ACTUAL);
        var employee = arrange(reservation, List.of(line(1, "1000", 0)));
        // Act
        var sale = service.sale(NUMBER, PHONE);
        // Assert
        assertNotNull(sale);
        assertEquals(SaleStatus.COMPLETED, sale.getStatus());
        assertEquals(ReservationStatus.INVALID, reservation.getStatus());
        assertSame(reservation, sale.getReservation());
        assertSame(reservation.getCustomer(), sale.getCustomer());
        assertSame(employee, sale.getEmployee());
        assertNotNull(sale.getSaleNum());
        assertFalse(sale.getSaleNum().isBlank());
        assertNotNull(sale.getCreatedAt());
        assertNotNull(sale.getPaidAt());
        assertMoney("1000", sale.getTotalAmount());
    }

    @Test
    void rejectsInactiveReservationWithoutChangingStock() {
        var reservation = reservation(ReservationStatus.INVALID);
        when(reservations.findByReservationNum(NUMBER)).thenReturn(reservation);
        // Current implementation looks up the employee before checking status.
        lenient().when(employees.findByPhone(PHONE)).thenReturn(new EmployeeEntity());

        var sale = service.sale(NUMBER, PHONE);

        assertNull(sale);
        verifyNoInteractions(reservationItems, products, saleItems, sales);
    }

    @Test
    void propagatesUnknownReservation() {
        when(reservations.findByReservationNum(NUMBER))
                .thenThrow(new IllegalArgumentException("Такой брони нет"));

        assertThrows(IllegalArgumentException.class, () -> service.sale(NUMBER, PHONE));

        verifyNoInteractions(employees, reservationItems, products, saleItems, sales);
    }

    @Test
    void propagatesUnknownEmployee() {
        when(reservations.findByReservationNum(NUMBER))
                .thenReturn(reservation(ReservationStatus.ACTUAL));
        when(employees.findByPhone(PHONE))
                .thenThrow(new IllegalArgumentException("Сотрудник не найден"));

        assertThrows(IllegalArgumentException.class, () -> service.sale(NUMBER, PHONE));

        verifyNoInteractions(reservationItems, products, saleItems, sales);
    }

    @Test
    void rejectsReservationWithoutItems() {
        var reservation = reservation(ReservationStatus.ACTUAL);
        arrange(reservation, List.of());

        assertThrows(IllegalArgumentException.class, () -> service.sale(NUMBER, PHONE));

        assertEquals(ReservationStatus.ACTUAL, reservation.getStatus());
        verifyNoInteractions(products);
        verify(saleItems, never()).save(any());
    }

    @Test
    void reducesStockAndTransfersItemData() {
        var reservation = reservation(ReservationStatus.ACTUAL);
        var line = line(2, "1000", 0);
        arrange(reservation, List.of(line));

        var sale = service.sale(NUMBER, PHONE);

        assertEquals(8, line.getProduct().getQuantityOnHand());
        assertEquals(0, line.getProduct().getQuantityReserved());
        verify(products).saveNotNew(line.getProduct());
        var saved = ArgumentCaptor.forClass(SaleItemEntity.class);
        verify(saleItems).save(saved.capture());
        assertSame(sale, saved.getValue().getSale());
        assertSame(line.getProduct(), saved.getValue().getProduct());
        assertEquals(Integer.valueOf(2), saved.getValue().getQuantity());
        assertMoney("1000", saved.getValue().getUnitPrice());
        assertMoney("1000", saved.getValue().getFinalUnitPrice());
    }

    @Test
    void appliesTenPercentDiscount() {
        arrange(reservation(ReservationStatus.ACTUAL), List.of(line(1, "1000", 0.10)));

        var sale = service.sale(NUMBER, PHONE);

        assertMoney("900", sale.getTotalAmount());
        var saved = ArgumentCaptor.forClass(SaleItemEntity.class);
        verify(saleItems).save(saved.capture());
        assertMoney("900", saved.getValue().getFinalUnitPrice());
    }

    @Test
    void retriesSaleNumberCollision() {
        arrange(reservation(ReservationStatus.ACTUAL), List.of(line(1, "1000", 0)));
        when(sales.existsBySaleNum(anyString())).thenReturn(true, false);

        var sale = service.sale(NUMBER, PHONE);

        assertNotNull(sale.getSaleNum());
        verify(sales, times(2)).existsBySaleNum(anyString());
    }

    @Test
    void repeatedSaleDoesNotReduceStockAgain() {
        var reservation = reservation(ReservationStatus.ACTUAL);
        var line = line(1, "1000", 0);
        arrange(reservation, List.of(line));

        var first = service.sale(NUMBER, PHONE);
        var second = service.sale(NUMBER, PHONE);

        assertNotNull(first);
        assertNull(second);
        assertEquals(9, line.getProduct().getQuantityOnHand());
        assertEquals(0, line.getProduct().getQuantityReserved());
        verify(saleItems, times(1)).save(any());
        verify(products, times(1)).saveNotNew(line.getProduct());
    }

    @Test
    void propagatesItemSaveFailure() {
        arrange(reservation(ReservationStatus.ACTUAL), List.of(line(1, "1000", 0)));
        var failure = new IllegalStateException("Ошибка сохранения позиции");
        doThrow(failure).when(saleItems).save(any(SaleItemEntity.class));

        var actual = assertThrows(IllegalStateException.class,
                () -> service.sale(NUMBER, PHONE));

        assertSame(failure, actual);
        // Transaction rollback requires an integration test with a real database.
    }

    // These expectations may expose an existing bug: total must include quantity.
    @Test
    void totalIncludesQuantity() {
        arrange(reservation(ReservationStatus.ACTUAL), List.of(line(2, "1000", 0)));

        var sale = service.sale(NUMBER, PHONE);

        assertMoney("2000", sale.getTotalAmount());
    }

    @Test
    void totalIncludesAllQuantitiesAndDiscounts() {
        arrange(reservation(ReservationStatus.ACTUAL), List.of(
                line(2, "1000", 0.10),
                line(3, "500", 0)));

        var sale = service.sale(NUMBER, PHONE);

        // 2 * 1000 * 0.90 + 3 * 500 = 3300.
        assertMoney("3300", sale.getTotalAmount());
        verify(saleItems, times(2)).save(any());
    }
}
