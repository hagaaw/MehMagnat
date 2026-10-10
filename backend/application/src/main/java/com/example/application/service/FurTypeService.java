package com.example.application.service;


import com.example.application.entity.forProduct.FurTypeEntity;
import com.example.application.repository.FurTypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class FurTypeService {
    private FurTypeRepository furTypeRepository;

    @Autowired
    public FurTypeService(FurTypeRepository furTypeRepository){
        this.furTypeRepository = furTypeRepository;
    }


    public List<FurTypeEntity> getAllFurTypes() {
        return furTypeRepository.findAll();
    }

    public Optional<FurTypeEntity> findByFurType(FurTypeEntity furType) {
        return furTypeRepository.findByName(furType.getName());
    }

    public void save(FurTypeEntity furType) {
        furTypeRepository.save(furType);
    }
}
