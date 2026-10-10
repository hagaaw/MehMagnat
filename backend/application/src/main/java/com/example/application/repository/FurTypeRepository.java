package com.example.application.repository;

import com.example.application.entity.forProduct.FurTypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FurTypeRepository extends JpaRepository<FurTypeEntity, Long> {
    Optional<FurTypeEntity> findByName(String name);
}
