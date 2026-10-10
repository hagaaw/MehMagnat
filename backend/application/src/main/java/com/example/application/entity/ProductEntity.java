package com.example.application.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name="products")
@Getter
@Setter
@NoArgsConstructor
public class ProductEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="id")
    private long id;

    @Column(name="sku", unique = true, nullable = false)
    private String sku;

    @Column(name="title",nullable = false)
    private String title;

    @Column(name="list_price", nullable = false)
    private BigDecimal listPrice;

    @Column(name="quantity_on_hand", nullable = false)
    private int quantityOnHand;

    @Column(name="quantity_reserved", nullable = false)
    private int quantityReserved;

    @Column(name="created_at", nullable = false)
    private LocalDateTime created_at;

    public ProductEntity(String sku, String title,
                         BigDecimal listPrice, int quantityOnHand,
                         int quantityReserved, LocalDateTime created_at) {
        this.sku = sku;
        this.title = title;
        this.listPrice = listPrice;
        this.quantityOnHand = quantityOnHand;
        this.quantityReserved = quantityReserved;
        this.created_at = created_at;
    }
}
