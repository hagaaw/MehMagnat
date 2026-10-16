package com.example.application.service.product;

import com.example.application.entity.forProduct.ColorEntity;
import com.example.application.repository.product.ColorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ColorService {
    private ColorRepository colorRepository;

    @Autowired
    public ColorService(ColorRepository colorRepository){
        this.colorRepository = colorRepository;
    }

    public Optional<ColorEntity> findByColor(ColorEntity color) {
        return colorRepository.findByName(color.getName());
    }

    public void save(ColorEntity color){
        colorRepository.save(color);
    }
}

