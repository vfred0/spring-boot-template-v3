package com.template.service.internal.address;

import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import com.template.api.dtos.customer.AddressDto;
import com.template.service.core.operations.bulk.BulkService;
import com.template.service.core.operations.route.ValidationRouteRegistry;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
public class AddressService extends BulkService<AddressDto, AddressDto> {

    public AddressService(Validator validator, ValidationRouteRegistry routeRegistry) {
        super(validator, routeRegistry);
    }


    private final AtomicInteger remainingErrors = new AtomicInteger(1);

    @Override
    protected void onValidCreate(AddressDto item) {
        log.info("Procesando item válido (create): {}", item.getId());

        if (remainingErrors.decrementAndGet() == 0) {
            log.warn("Simulating exception for item: {}", item.getId());
//            throw new BulkItemProcessingException("item", null, "[]", new InternalServerErrorException("Simulated conflict error for item " + item.getId()));
//            throw   new InternalServerErrorException("Simulated conflict error for item " + item.getId());
        }

        try {
            log.info("Simulating long processing for item: {}", item.getId());
            Thread.sleep(1);
            log.info("Finished processing item: {}", item.getId());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    protected void onValidPatch(AddressDto item) {
        log.info("Procesando item válido (patch): {}", item.getId());
    }
}
