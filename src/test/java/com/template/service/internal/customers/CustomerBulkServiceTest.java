package com.template.service.internal.customers;

import com.template.api.dtos.customer.AddressDto;
import com.template.api.dtos.customer.CustomerRequestDto;
import com.template.api.dtos.customer.CustomerStatus;
import com.template.service.core.mapper.Mapper;
import com.template.service.core.operations.route.ValidationRouteRegistry;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CustomerBulkServiceTest {

    @Mock
    private Validator validator;

    @Mock
    private ValidationRouteRegistry routeRegistry;

    @Mock
    private Mapper mapper;

    @Mock
    private CustomerBulkCreateDispatcher createDispatcher;

    private CustomerBulkService customerBulkService;

    @BeforeEach
    void setUp() {
        customerBulkService = new CustomerBulkService(validator, routeRegistry, mapper, createDispatcher);
    }

    @Test
    void onValidCreate_schedulesDurableTaskWithDtoAsPayload() {
        CustomerRequestDto dto = buildValidDto("cust-1");

        customerBulkService.onValidCreate(dto);

        verify(createDispatcher).schedule(dto);
    }

    private CustomerRequestDto buildValidDto(String id) {
        return CustomerRequestDto.builder()
                .odooId("odoo-" + id)
                .id(id)
                .dni("1234567890")
                .names("Integration Test Customer")
                .creditLimit(100.0)
                .isArchived(false)
                .status(CustomerStatus.NEW)
                .addresses(Set.of(AddressDto.builder().id("addr-1").address("Calle Falsa 123").build()))
                .build();
    }
}
