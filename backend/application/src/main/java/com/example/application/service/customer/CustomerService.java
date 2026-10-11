package com.example.application.service.customer;


import com.example.application.entity.forCustomer.CustomerEntity;
import com.example.application.repository.customer.CustomerRepository;
import org.springframework.stereotype.Service;

@Service
public class CustomerService {


    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public CustomerEntity saveDB(CustomerEntity customer) {
        var searchCustomer = customerRepository.findByPhoneAndEmail(customer.getPhone(),customer.getEmail());
        if (searchCustomer.isEmpty()){
            customerRepository.save(customer);
        }
        return customer;
    }
}
