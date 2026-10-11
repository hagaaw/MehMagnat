package com.example.application.repository.customer;

import com.example.application.entity.forCustomer.CustomerEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<CustomerEntity, Long> {
}
