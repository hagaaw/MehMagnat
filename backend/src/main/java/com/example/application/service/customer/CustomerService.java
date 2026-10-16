package com.example.application.service.customer;


import com.example.application.dto.ListProductsAndCostumerDto;
import com.example.application.entity.forCustomer.CustomerEntity;
import com.example.application.entity.forProduct.ProductEntity;
import com.example.application.entity.forReservation.ReservationEntity;
import com.example.application.entity.forReservation.ReservationItemEntity;
import com.example.application.entity.forReservation.ReservationStatus;
import com.example.application.repository.customer.CustomerRepository;
import com.example.application.service.product.ProductService;
import com.example.application.service.reservation.ReservationItemService;
import com.example.application.service.reservation.ReservationService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CustomerService {


    private final CustomerRepository customerRepository;
    private final ProductService productService;
    private final ReservationItemService reservationItemService;

    public CustomerService(CustomerRepository customerRepository,ProductService productService, ReservationItemService reservationItemService) {
        this.customerRepository = customerRepository;
        this.productService = productService;
        this.reservationItemService = reservationItemService;
    }

    public CustomerEntity saveDB(CustomerEntity customer) {
        var searchCustomer = customerRepository.findByPhoneAndEmail(customer.getPhone(),customer.getEmail());
        if (searchCustomer.isEmpty()){
            customerRepository.save(customer);
            return customer;
        }
        return searchCustomer.get();
    }


    public CustomerEntity findById(Long customerId) {
        return customerRepository.findById(customerId).orElseThrow(() -> new IllegalArgumentException("такого покупателя нет"));
    }
}
