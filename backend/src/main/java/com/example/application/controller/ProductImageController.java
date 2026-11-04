package com.example.application.controller;

import com.example.application.entity.forProduct.ProductEntity;
import com.example.application.service.product.ProductService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;


@Tag(name = "product-images")
@RestController
@RequestMapping("/api/products/image")
public class ProductImageController {

    private static final String URL_PREFIX = "/uploads/";
    private static final long MAX_SIZE = 5L * 1024 * 1024; // 5 МБ

    private final ProductService productService;
    private final Path uploadDir;
    private final Logger logger = LoggerFactory.getLogger(ProductImageController.class);

    public ProductImageController(ProductService productService,
                                  @Value("${app.upload.dir:/uploads}") String uploadDir) {
        this.productService = productService;
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> upload(@RequestParam String sku,
                                    @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return error(HttpStatus.BAD_REQUEST, "Файл пустой.");
        }
        if (file.getSize() > MAX_SIZE) {
            return error(HttpStatus.BAD_REQUEST, "Файл больше 5 МБ.");
        }

        ProductEntity product;
        try {
            product = productService.findBySku(sku);
        } catch (IllegalArgumentException e) {
            return error(HttpStatus.NOT_FOUND, "Товар не найден.");
        }

        try {

            String extension = detectExtension(file);
            if (extension == null) {
                return error(HttpStatus.BAD_REQUEST, "Допустимы только изображения JPG, PNG или WebP.");
            }

            Files.createDirectories(uploadDir);
            String fileName = UUID.randomUUID() + "." + extension;
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, uploadDir.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
            }

            String oldUrl = product.getImageUrl();
            product.setImageUrl(URL_PREFIX + fileName);
            productService.saveNotNew(product);
            deleteFileByUrl(oldUrl); // старое фото больше не нужно

            logger.info("Загружено фото для товара {}: {}", sku, fileName);
            return ResponseEntity.ok(product);
        } catch (IOException e) {
            logger.error("Не удалось сохранить фото товара {}", sku, e);
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "Не удалось сохранить файл на сервере.");
        }
    }

    @DeleteMapping
    public ResponseEntity<?> delete(@RequestParam String sku) {
        ProductEntity product;
        try {
            product = productService.findBySku(sku);
        } catch (IllegalArgumentException e) {
            return error(HttpStatus.NOT_FOUND, "Товар не найден.");
        }

        deleteFileByUrl(product.getImageUrl());
        product.setImageUrl(null);
        productService.saveNotNew(product);
        return ResponseEntity.ok(product);
    }




    private String detectExtension(MultipartFile file) throws IOException {
        byte[] head = new byte[12];
        int read;
        try (InputStream in = file.getInputStream()) {
            read = in.readNBytes(head, 0, head.length);
        }
        if (read >= 3 && (head[0] & 0xFF) == 0xFF && (head[1] & 0xFF) == 0xD8 && (head[2] & 0xFF) == 0xFF) {
            return "jpg";
        }
        if (read >= 8 && (head[0] & 0xFF) == 0x89 && head[1] == 'P' && head[2] == 'N' && head[3] == 'G'
                && head[4] == 0x0D && head[5] == 0x0A && head[6] == 0x1A && head[7] == 0x0A) {
            return "png";
        }
        if (read >= 12 && head[0] == 'R' && head[1] == 'I' && head[2] == 'F' && head[3] == 'F'
                && head[8] == 'W' && head[9] == 'E' && head[10] == 'B' && head[11] == 'P') {
            return "webp";
        }
        return null;
    }


    private void deleteFileByUrl(String url) {
        if (url == null || !url.startsWith(URL_PREFIX)) return;
        try {
            Path name = Paths.get(url.substring(URL_PREFIX.length())).getFileName();
            if (name != null) {
                Files.deleteIfExists(uploadDir.resolve(name.toString()));
            }
        } catch (Exception e) {
            logger.warn("Не удалось удалить старый файл {}", url, e);
        }
    }


    private ResponseEntity<Map<String, String>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("message", message));
    }
}
