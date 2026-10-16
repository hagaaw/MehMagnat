package com.example.application.repository.product;

import com.example.application.entity.forProduct.ColorEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ColorRepository extends JpaRepository<ColorEntity, Long> {
    Optional<ColorEntity> findByName(String name);
}
