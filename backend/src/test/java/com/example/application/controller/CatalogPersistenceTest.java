package com.example.application.controller;

import com.example.application.entity.forProduct.*;
import com.example.application.repository.product.*;
import com.example.application.service.product.*;
import com.example.application.service.reservation.ReservationItemService;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CatalogPersistenceTest {
    final ProductRepository products = mock(ProductRepository.class);
    final SizeRepository sizes = mock(SizeRepository.class);
    final ColorRepository colors = mock(ColorRepository.class);
    final FurTypeRepository furs = mock(FurTypeRepository.class);
    final FurTypeService furTypes = new FurTypeService(furs);
    final ProductService service = new ProductService(products,new ColorService(colors),new SizeService(sizes),furTypes);
    final ProductController controller = new ProductController(service,mock(ReservationItemService.class));

    @Test void newCoatCreatesMissingDictionaryEntries() {
        var coat = new ProductEntity("Шуба",BigDecimal.TEN,3,0,LocalDateTime.now(),Status.FREE);
        var size = new SizeEntity("M","Средний");
        var color = new ColorEntity("Белый");
        var fur = new FurTypeEntity("Норка");
        coat.setSize(size);
        coat.setColor(color);
        coat.setFurType(fur);
        var result = controller.addProduct(coat);
        assertSame(coat,result.getBody());
        assertFalse(coat.getSku().isBlank());
        verify(sizes).save(size);
        verify(colors).save(color);
        verify(furs).save(fur);
        verify(products).save(coat);
    }

    @Test void updateUsesExistingDictionaryEntriesAndKeepsSku() {
        var date = LocalDateTime.of(2026,1,1,12,0);
        var current = new ProductEntity("M1","Шуба",BigDecimal.TEN,3,0,date,Status.FREE);
        var updated = new ProductEntity("Новое имя",BigDecimal.ONE,2,1,date,Status.BOOKED);
        var size = new SizeEntity("M","Средний");
        var color = new ColorEntity("Белый");
        var fur = new FurTypeEntity("Норка");
        updated.setSize(size);
        updated.setColor(color);
        updated.setFurType(fur);
        when(products.findBySku("M1")).thenReturn(Optional.of(current));
        when(sizes.findAllByName("M")).thenReturn(Optional.of(size));
        when(colors.findByName("Белый")).thenReturn(Optional.of(color));
        when(furs.findByName("Норка")).thenReturn(Optional.of(fur));
        when(products.save(current)).thenReturn(current);
        var result = controller.putProduct("M1",updated).getBody();
        assertEquals("M1",result.getSku());
        assertEquals("Новое имя",result.getTitle());
        assertEquals(BigDecimal.ONE,result.getListPrice());
        assertEquals(1,result.getQuantityReserved());
        verify(furs,never()).save(any());
    }

    @Test void dictionaryReturnsPersistedFurTypes() {
        var mink = new FurTypeEntity("Норка");
        when(furs.findAll()).thenReturn(List.of(mink));
        assertEquals(List.of(mink),new FurTypeController(furTypes).getAllFurTypes().getBody());
    }
}
