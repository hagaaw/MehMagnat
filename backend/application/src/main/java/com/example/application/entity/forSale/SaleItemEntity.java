package com.example.application.entity.forSale;


import com.example.application.entity.forCustomer.CustomerEntity;
import com.example.application.entity.forEmployee.EmployeeEntity;
import com.example.application.entity.forProduct.ProductEntity;
import com.example.application.entity.forReservation.ReservationEntity;
import jakarta.persistence.*;
import jakarta.persistence.criteria.CriteriaBuilder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name="sales_items")
@Getter
@Setter
@NoArgsConstructor
public class SaleItemEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;



    @OneToOne
    @JoinColumn(name="sale_id",nullable = false)
    private SaleEntity sale;

    @OneToOne
    @JoinColumn(name="product_id", nullable = false)
    private ProductEntity product;



    @Column(name="quantity", nullable = false)
    private Integer quantity;

    @Column(name="unit_price", nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "discount_amount", nullable = false)
    private Double discountAmount; //скидка за единицу

    @Column(name = "final_unit_price", nullable = false)
    private Double finalUnitPrice; //цена за единицу с учетом скидки



}
