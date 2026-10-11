package com.example.application.entity.forReservation;


import com.example.application.entity.forProduct.ProductEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.context.annotation.Configuration;

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
    private Integer unitPrice; //скидка за штуку

    @Column(name = "discount_amount", nullable = false)
    private Double discountAmount; //скидка за единицу


    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name="reservation_id", nullable = false)
    private ReservationEntity reservation;

    @ManyToOne
    @JoinColumn(name="product_id", nullable = false)
    private ProductEntity product;

}
