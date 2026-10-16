package com.example.application.entity.forReservation;


import com.example.application.entity.forProduct.ProductEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Entity
@Table(name="reservations_items")
@Getter
@Setter
@NoArgsConstructor
public class ReservationItemEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name="quantity", nullable = false)
    private Integer quantity;

    @Column(name="unit_price", nullable = false)
    private BigDecimal unitPrice; //цена за штуку

    @Column(name = "discount_amount", nullable = false)
    private Double discountAmount; //скидка за единицу


    @ManyToOne
    @JoinColumn(name="reservation_id", nullable = false)
    private ReservationEntity reservation;

    @ManyToOne
    @JoinColumn(name="product_id", nullable = false)
    private ProductEntity product;

    public ReservationItemEntity(Integer quantity,
                                 BigDecimal unitPrice,
                                 Double discountAmount,
                                 ReservationEntity reservation,
                                 ProductEntity product) {
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.discountAmount = discountAmount;
        this.reservation = reservation;
        this.product = product;
    }
}
