package com.template.service.internal.customers;


import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import com.template.api.dtos.customer.CustomerPatchDto;
import com.template.api.dtos.customer.CustomerRequestDto;
import com.template.api.http_errors.exceptions.InternalServerErrorException;
import com.template.data.entities.Address;
import com.template.data.entities.Customer;
import com.template.data.entities.PaymentTerm;
import com.template.service.core.mapper.Mapper;
import com.template.service.core.mapper.NestedPatchDef;
import com.template.service.core.operations.bulk.BulkService;
import com.template.service.core.operations.route.ValidationRouteRegistry;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
public class CustomerBulkService extends BulkService<CustomerRequestDto, CustomerPatchDto> {

    private final Mapper mapper;
    private final AtomicInteger remainingErrors = new AtomicInteger(1);

    public CustomerBulkService(Validator validator, ValidationRouteRegistry routeRegistry, Mapper mapper) {
        super(validator, routeRegistry);
        this.mapper = mapper;
    }

    @Override
    protected void onValidCreate(CustomerRequestDto dto) {
        log.info("Processing processed dto (create): {}", dto.getId());
        var customer = this.mapper.mapToCreate(dto, new Customer());
        if (remainingErrors.decrementAndGet() == 0) {
            log.warn("Simulating exception for item: {}", dto.getId());
//            throw new BulkItemProcessingException("dto", null, "[]", new InternalServerErrorException("Simulated conflict error for dto " + dto.getId()));
            throw   new InternalServerErrorException("Simulated conflict error for dto " + dto.getId());
        }

        log.info("Mapped customer: {}", customer);
    }

    @Override
    protected void onValidPatch(CustomerPatchDto dto) {
        var existingCustomer = Customer.builder()
                .id("12334")
                .odooId("12345-Odoo")
                .names("Victor Arreaga")
                .creditLimit(2999.1)
                .addresses(new HashSet<>(Set.of(
                        Address.builder()
                                .id("12345")
                                .address("Calle Falsa 123")
                                .province("Buenos Aires")
                                .city("Springfield")
                                .build()
                )))
                .paymentTerm(PaymentTerm.builder().id(1).name("Prueba de termino").build())
                .build();

        var patchedCustomer = this.mapper.mapToPatch(
                dto,
                existingCustomer,
                "id",
                NestedPatchDef.of("addresses", "id", Address.class),
                NestedPatchDef.of("paymentTerm", "id", PaymentTerm.class)
        );

        log.info("Mapped customer: {}", patchedCustomer);
    }
}
