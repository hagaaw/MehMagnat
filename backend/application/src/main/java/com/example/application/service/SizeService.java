package com.example.application.service;


import com.example.application.entity.forProduct.SizeEntity;
import com.example.application.repository.SizeRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class SizeService {
    private SizeRepository sizeRepository;

    public SizeService(SizeRepository sizeRepository){
        this.sizeRepository = sizeRepository;
    }

    public Optional<SizeEntity> findBySize(SizeEntity size) {
        return sizeRepository.findAllByName(size.getName());
    }

    public void save(SizeEntity size) {
        sizeRepository.save(size);
    }
}
