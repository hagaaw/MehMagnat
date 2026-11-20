package com.example.application.service.product;

import com.example.application.entity.forProduct.*;
import com.example.application.repository.product.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {
    @Mock ProductRepository products;
    @Mock SizeService sizes;
    @Mock ColorService colors;
    @Mock FurTypeService furs;
    @InjectMocks ProductService service;

    private ProductEntity coat() {
        ProductEntity product = new ProductEntity();
        product.setTitle("Шуба");
        product.setListPrice(new BigDecimal("35000"));
        product.setQuantityOnHand(5);
        product.setQuantityReserved(2);
        product.setStatus(Status.FREE);
        product.setSize(new SizeEntity());
        product.setColor(new ColorEntity());
        product.setFurType(new FurTypeEntity());
        return product;
    }

    @ParameterizedTest
    @ValueSource(strings = {"size", "color", "furType"})
    void rejectsMissingAttributeBeforeSaving(String attribute) {
        ProductEntity product = coat();
        switch (attribute) {
            case "size" -> product.setSize(null);
            case "color" -> product.setColor(null);
            case "furType" -> product.setFurType(null);
        }
        var error = assertThrows(IllegalArgumentException.class, () -> service.saveToDB(product));
        assertEquals(attribute + " is required", error.getMessage());
        verifyNoInteractions(products, sizes, colors, furs);
    }

    @Test
    void usesExistingAttributesAndRetriesOccupiedSku() {
        ProductEntity product = coat();
        ProductEntity canonical = coat();
        when(sizes.findBySize(product.getSize())).thenReturn(Optional.of(canonical.getSize()));
        when(colors.findByColor(product.getColor())).thenReturn(Optional.of(canonical.getColor()));
        when(furs.findByFurType(product.getFurType())).thenReturn(Optional.of(canonical.getFurType()));
        when(products.existsBySku(anyString())).thenReturn(true, false);

        service.saveToDB(product);

        verify(products, times(2)).existsBySku(anyString());
        assertFalse(product.getSku().isBlank());
        assertSame(canonical.getSize(), product.getSize());
        assertSame(canonical.getColor(), product.getColor());
        assertSame(canonical.getFurType(), product.getFurType());
        verify(products).save(product);
    }

    @Test
    void updatesExistingProductAndCreatesMissingAttributes() {
        ProductEntity stored = new ProductEntity();
        stored.setSku("A1");
        ProductEntity request = coat();
        when(products.findBySku("A1")).thenReturn(Optional.of(stored));
        when(sizes.save(request.getSize())).thenReturn(request.getSize());
        when(colors.save(request.getColor())).thenReturn(request.getColor());
        when(furs.save(request.getFurType())).thenReturn(request.getFurType());
        when(products.save(stored)).thenReturn(stored);

        assertSame(stored, service.putProduct("A1", request));
        assertEquals("A1", stored.getSku());
        assertEquals(request.getTitle(), stored.getTitle());
        assertEquals(request.getListPrice(), stored.getListPrice());
        assertEquals(5, stored.getQuantityOnHand());
        assertEquals(2, stored.getQuantityReserved());
        assertEquals(Status.FREE, stored.getStatus());
        assertSame(request.getSize(), stored.getSize());
        assertSame(request.getColor(), stored.getColor());
        assertSame(request.getFurType(), stored.getFurType());
    }

    @Test
    void rejectsUpdateForUnknownSku() {
        assertThrows(IllegalArgumentException.class, () -> service.putProduct("missing", coat()));
        verify(products, never()).save(any());
        verifyNoInteractions(colors, sizes, furs);
    }

    @Test
    void looksUpProductBySku() {
        ProductEntity product = coat();
        when(products.findBySku("A1")).thenReturn(Optional.of(product));
        assertSame(product, service.findBySku("A1"));
        assertThrows(IllegalArgumentException.class, () -> service.findBySku("missing"));
    }
}
