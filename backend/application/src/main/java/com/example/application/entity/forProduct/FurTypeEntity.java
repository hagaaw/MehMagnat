package com.example.application.entity.forProduct;


import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Entity
@Table(name="fur_types")
@Getter
@Setter
@NoArgsConstructor
public class FurTypeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="id")
    private long id;

    @Column(name="name", nullable = false)
    private String name;


    @OneToMany(mappedBy = "furType")
    private List<ProductEntity> products;

    public FurTypeEntity(String name) {
        this.name = name;
    }
}
