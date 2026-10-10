package com.example.application.dto;

import com.example.application.entity.forProduct.ColorEntity;
import com.example.application.entity.forProduct.FurTypeEntity;
import com.example.application.entity.forProduct.SizeEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductRequest {
    private FurTypeEntity furType;
    private ColorEntity color;
    private SizeEntity size;
}
