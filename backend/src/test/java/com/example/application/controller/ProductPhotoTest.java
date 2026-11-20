package com.example.application.controller;

import com.example.application.entity.forProduct.ProductEntity;
import com.example.application.service.product.ProductService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockMultipartFile;
import java.nio.file.*;
import java.util.HexFormat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProductPhotoTest {
    @TempDir Path dir;
    final ProductService products = mock(ProductService.class);

    @ParameterizedTest
    @CsvSource({"jpg,ffd8ff","png,89504e470d0a1a0a","webp,524946460000000057454250"})
    void replacesPhotoAccordingToSignature(String extension, String hex) throws Exception {
        var controller = new ProductImageController(products, dir.toString());
        var product = new ProductEntity();
        Path old = Files.writeString(dir.resolve("old.jpg"), "old");
        product.setImageUrl("/uploads/old.jpg");
        when(products.findBySku("A")).thenReturn(product);
        byte[] bytes = HexFormat.of().parseHex(hex);
        var result = controller.upload("A", new MockMultipartFile("file", bytes));
        assertEquals(200, result.getStatusCode().value());
        assertSame(product, result.getBody());
        assertTrue(product.getImageUrl().startsWith("/uploads/"));
        assertTrue(product.getImageUrl().endsWith("." + extension));
        Path saved = dir.resolve(Path.of(product.getImageUrl()).getFileName());
        assertArrayEquals(bytes, Files.readAllBytes(saved));
        assertFalse(Files.exists(old));
        verify(products).saveNotNew(product);
    }

    @ParameterizedTest
    @CsvSource({"0,400","5242881,400"})
    void rejectsEmptyOrOversizedPhoto(int size, int status) {
        var controller = new ProductImageController(products, dir.toString());
        assertEquals(status, controller.upload("A", new MockMultipartFile("file", new byte[size])).getStatusCode().value());
        verifyNoInteractions(products);
    }

    @Test void rejectsUnknownFormatWithoutChangingPhoto() {
        var controller = new ProductImageController(products, dir.toString());
        var product = new ProductEntity();
        product.setImageUrl("/uploads/old.jpg");
        when(products.findBySku("A")).thenReturn(product);
        var response = controller.upload("A", new MockMultipartFile("file", new byte[]{1,2,3}));
        assertEquals(400, response.getStatusCode().value());
        assertEquals("/uploads/old.jpg", product.getImageUrl());
        verify(products, never()).saveNotNew(any());
    }

    @Test void missingProductHasNoPhotoToChange() {
        var controller = new ProductImageController(products, dir.toString());
        when(products.findBySku("X")).thenThrow(new IllegalArgumentException());
        assertEquals(404, controller.upload("X", new MockMultipartFile("file", new byte[]{1})).getStatusCode().value());
        assertEquals(404, controller.delete("X").getStatusCode().value());
    }

    @Test void removesExistingPhotoAndClearsReference() throws Exception {
        var controller = new ProductImageController(products, dir.toString());
        Path old = Files.writeString(dir.resolve("coat.jpg"), "old");
        var product = new ProductEntity();
        product.setImageUrl("/uploads/coat.jpg");
        when(products.findBySku("A")).thenReturn(product);
        assertSame(product, controller.delete("A").getBody());
        assertFalse(Files.exists(old));
        assertNull(product.getImageUrl());
        verify(products).saveNotNew(product);
    }

    @Test void unavailableDirectoryLeavesProductUnchanged() throws Exception {
        Path occupied = Files.createFile(dir.resolve("occupied"));
        var controller = new ProductImageController(products, occupied.toString());
        var product = new ProductEntity();
        when(products.findBySku("A")).thenReturn(product);
        var file = new MockMultipartFile("file", HexFormat.of().parseHex("ffd8ff"));
        assertEquals(500, controller.upload("A", file).getStatusCode().value());
        verify(products, never()).saveNotNew(any());
    }
}
