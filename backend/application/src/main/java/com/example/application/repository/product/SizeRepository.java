package com.example.application.repository.product;

import com.example.application.entity.forProduct.SizeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SizeRepository extends JpaRepository<SizeEntity, Long> {
    Optional<SizeEntity> findAllByName(String name);
}
