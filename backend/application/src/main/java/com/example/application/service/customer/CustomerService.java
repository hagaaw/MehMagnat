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

@Service
public class CustomerService {


    private final CustomerRepository customerRepository;
    private final ReservationService reservationService;
    private final ProductService productService;
    private final ReservationItemService reservationItemService;

    public CustomerService(CustomerRepository customerRepository, ReservationService reservationService, ProductService productService, ReservationItemService reservationItemService) {
        this.customerRepository = customerRepository;
        this.reservationService = reservationService;
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

    public Boolean madeReservation(ListProductsAndCostumerDto listProductsAndCostumerDto) {
        int count = 0;
        var customer = saveDB(listProductsAndCostumerDto.getCustomer());

        ReservationEntity reservation = reservationService.getNewReservation(customer,
                listProductsAndCostumerDto.getStartAt(),
                listProductsAndCostumerDto.getEndAt());

        for(var dtoProduct: listProductsAndCostumerDto.getSkuAndCount()){
            ProductEntity product = productService.findBySku(dtoProduct.getSku());
            if (productService.findBySku(dtoProduct.getSku()).getQuantityReserved()
                    < dtoProduct.getCount()){
                continue;
            }
            Integer countBusy = reservationItemService.checkOnValid(dtoProduct.getSku(),
                    listProductsAndCostumerDto.getStartAt(),
                    listProductsAndCostumerDto.getEndAt());

            if ( countBusy == null){
                countBusy = 0;
            }


            if (-countBusy + product.getQuantityReserved() >= dtoProduct.getCount()){

                ReservationItemEntity reservationItemEntity = new ReservationItemEntity(
                        dtoProduct.getCount(),
                        product.getListPrice(),
                        0d,
                        reservation,
                        product
                );
                reservationItemService.save(reservationItemEntity);
                count++;
            }
        }
        if (count != 0){
            reservation.setStatus(ReservationStatus.ACTUAL);
            return true;
        }
        return false;
    }
}
