package com.example.application.service.customer;

import com.example.application.entity.forCustomer.CustomerEntity;
import com.example.application.repository.customer.CustomerRepository;
import com.example.application.service.product.ProductService;
import com.example.application.service.reservation.ReservationItemService;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CustomerServiceTest {
    private final CustomerRepository repository = mock(CustomerRepository.class);
    private final CustomerService service = new CustomerService(repository,
        mock(ProductService.class), mock(ReservationItemService.class));

    private CustomerEntity contact() {
        CustomerEntity customer = new CustomerEntity();
        customer.setPhone("+79000000001");
        customer.setEmail("test@example.org");
        return customer;
    }

    @Test void newContactIsSaved() {
        CustomerEntity customer = contact();
        assertSame(customer, service.saveDB(customer));
        verify(repository).findByPhoneAndEmail(customer.getPhone(), customer.getEmail());
        verify(repository).save(customer);
    }

    @Test void existingContactIsReturnedWithoutDuplicate() {
        CustomerEntity request = contact();
        CustomerEntity saved = contact();
        when(repository.findByPhoneAndEmail(request.getPhone(), request.getEmail()))
            .thenReturn(Optional.of(saved));
        assertSame(saved, service.saveDB(request));
        verify(repository, never()).save(any());
    }

    @Test void lookupByIdReturnsCustomerOrReportsMissingId() {
        CustomerEntity customer = contact();
        when(repository.findById(1L)).thenReturn(Optional.of(customer));
        assertSame(customer, service.findById(1L));
        assertThrows(IllegalArgumentException.class, () -> service.findById(2L));
    }

    @Test void lookupByPhoneReturnsCustomerOrReportsMissingPhone() {
        CustomerEntity customer = contact();
        when(repository.findByPhone(customer.getPhone())).thenReturn(Optional.of(customer));
        assertSame(customer, service.findByPhone(customer.getPhone()));
        assertThrows(IllegalArgumentException.class, () -> service.findByPhone("+79000000002"));
    }
}
