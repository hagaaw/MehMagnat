package com.example.application.service.sale;


import com.example.application.entity.forCustomer.CustomerEntity;
import com.example.application.entity.forEmployee.EmployeeEntity;
import com.example.application.entity.forReservation.ReservationEntity;
import com.example.application.entity.forReservation.ReservationItemEntity;
import com.example.application.entity.forReservation.ReservationStatus;
import com.example.application.entity.forSale.SaleEntity;
import com.example.application.entity.forSale.SaleItemEntity;
import com.example.application.entity.forSale.SaleStatus;
import com.example.application.repository.sale.SaleItemRepository;
import com.example.application.repository.sale.SaleRepository;
import com.example.application.service.customer.CustomerService;
import com.example.application.service.employee.EmployeeService;
import com.example.application.service.product.ProductService;
import com.example.application.service.reservation.ReservationItemService;
import com.example.application.service.reservation.ReservationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static com.example.application.config.AppConfig.generateRandomString;
import static com.example.application.config.AppConfig.len;
@Transactional
@Service
public class SaleService {
    private final ReservationService reservationService;
    private final ReservationItemService reservationItemService;
    private final SaleRepository saleRepository;
    private final EmployeeService employeeService;
    private final CustomerService customerService;
    private final SaleItemRepository saleItemRepository;
    private final SaleItemService saleItemService;
    private final ProductService productService;

    public SaleService(ReservationService reservationService, ReservationItemService reservationItemService, SaleRepository saleRepository, EmployeeService employeeService, CustomerService customerService, SaleItemRepository saleItemRepository, SaleItemService saleItemService, ProductService productService) {
        this.reservationService = reservationService;
        this.reservationItemService = reservationItemService;
        this.saleRepository = saleRepository;
        this.employeeService = employeeService;
        this.customerService = customerService;
        this.saleItemRepository = saleItemRepository;
        this.saleItemService = saleItemService;
        this.productService = productService;
    }

    public SaleEntity sale(String reservationNum, String phoneEmployee){
        BigDecimal sum = new BigDecimal(0);
        LocalDateTime createdAt = LocalDateTime.now();
        LocalDateTime paidAt = LocalDateTime.now();
        ReservationEntity reservation = reservationService.findByReservationNum(reservationNum);
        EmployeeEntity employee = employeeService.findByPhone(phoneEmployee);
        CustomerEntity customer = reservation.getCustomer();

        if(reservation.getStatus().equals(ReservationStatus.INVALID)){
            return null;
        }


        List<ReservationItemEntity> reservationItemList = reservationItemService.findAllByReservationNum(reservation.getReservationNum());
        for(var reservationItem: reservationItemList){
            sum = sum.add(reservationItem.getUnitPrice().multiply(BigDecimal.valueOf(1 - reservationItem.getDiscountAmount())));
        }

        SaleEntity sale = new SaleEntity(generatedUniqueNum(),
                customer,
                employee,
                SaleStatus.PENDING,
                reservation,
                sum,
                paidAt,
                createdAt);

        int count = 0;

        for(var reservationItem: reservationItemList){
            var product = reservationItem.getProduct();
            var quantity = reservationItem.getQuantity();
            SaleItemEntity saleItem = new SaleItemEntity(
                    sale,
                    product,
                    quantity,
                    reservationItem.getUnitPrice(),
                    reservationItem.getDiscountAmount(),
                    reservationItem.getUnitPrice().multiply(BigDecimal.valueOf(1 - reservationItem.getDiscountAmount()))
            );
            product.setQuantityReserved(product.getQuantityReserved() - quantity);
            productService.saveNotNew(product);
            saleItemService.save(saleItem);
            count++;

        }

        if (count == 0){
            saleRepository.delete(sale);
            saleItemService.deleteAllBySale(sale);
            throw new IllegalArgumentException("В данной брони нет товаров");
        }else {
            reservation.setStatus(ReservationStatus.INVALID);
            sale.setStatus(SaleStatus.COMPLETED);
            return sale;
        }
    }


    private String generatedUniqueNum() {
        String saleNum;
        do {
            saleNum = generateRandomString(len);
        } while (saleRepository.existsBySaleNum(saleNum));
        return saleNum;
    }
}
