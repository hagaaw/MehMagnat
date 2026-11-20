package com.example.application.controller;

import com.example.application.dto.ListProductsAndCostumerDto;
import com.example.application.entity.forProduct.ProductEntity;
import com.example.application.service.customer.CustomerService;
import com.example.application.service.product.ProductService;
import com.example.application.service.reservation.ReservationItemService;
import com.example.application.service.reservation.ReservationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CatalogAndBookingHttpTest {
    private final ProductService products = mock(ProductService.class);
    private final ReservationService bookings = mock(ReservationService.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(
        new ProductController(products, mock(ReservationItemService.class)),
        new ReservationController(mock(CustomerService.class), bookings)).build();

    @Test void emptyCatalogReturnsNotFound() throws Exception {
        when(products.getAll()).thenReturn(List.of());
        mvc.perform(get("/api/products/get_all")).andExpect(status().isNotFound());
    }

    @Test void catalogReturnsSku() throws Exception {
        ProductEntity coat = new ProductEntity();
        coat.setSku("M1");
        when(products.getAll()).thenReturn(List.of(coat));
        mvc.perform(get("/api/products/get_all")).andExpect(status().isOk())
            .andExpect(jsonPath("$[0].sku").value("M1"));
    }

    @ParameterizedTest
    @CsvSource({"0,0,0","1,0,1","1,1,0","2,0,2","2,1,1","2,2,0"})
    void availableStock(int stock, int reserved, int expected) throws Exception {
        ProductEntity coat = new ProductEntity();
        coat.setQuantityOnHand(stock);
        coat.setQuantityReserved(reserved);
        when(products.findBySku("M1")).thenReturn(coat);
        mvc.perform(get("/api/products/get/sku/M1")).andExpect(status().isOk())
            .andExpect(content().string(String.valueOf(expected)));
    }

    @Test void bookingPassesItemAndReturnsNumber() throws Exception {
        when(bookings.madeReservation(any())).thenReturn(Optional.of("R7"));
        mvc.perform(post("/api/reservation/made_reservation").contentType("application/json")
            .content("{\"skuAndCount\":[{\"sku\":\"M1\",\"count\":2}]}"))
            .andExpect(status().isOk()).andExpect(content().string("R7"));
        var dto = ArgumentCaptor.forClass(ListProductsAndCostumerDto.class);
        verify(bookings).madeReservation(dto.capture());
        var items = dto.getValue().getSkuAndCount();
        assertEquals(1, items.size());
        assertEquals("M1", items.getFirst().getSku());
        assertEquals(2, items.getFirst().getCount());
    }

    @Test void malformedBookingDoesNotReachService() throws Exception {
        mvc.perform(post("/api/reservation/made_reservation").contentType("application/json")
            .content("{")).andExpect(status().isBadRequest());
        verifyNoInteractions(bookings);
    }
}
