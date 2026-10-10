package com.example.application.entity.forProduct;


import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Entity
@Table(name="sizes")
@Getter
@Setter
@NoArgsConstructor
public class SizeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="id")
    private long id;

    @Column(name="name", nullable = false)
    private String name;

    @Column(name="description", nullable = false)
    private String description;

    @OneToMany(mappedBy = "size")
    private List<ProductEntity> products;

    public SizeEntity(String name, String description) {
        this.name = name;
        this.description = description;
    }
}
