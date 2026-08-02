package com.template.service.internal.customers;

import com.github.kagkarlsson.scheduler.boot.config.DbSchedulerCustomizer;
import com.github.kagkarlsson.scheduler.serializer.JacksonSerializer;
import com.github.kagkarlsson.scheduler.serializer.Serializer;
import com.github.kagkarlsson.scheduler.task.TaskDescriptor;
import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import com.template.api.dtos.customer.CustomerRequestDto;
import com.template.api.http_errors.exceptions.InternalServerErrorException;
import com.template.data.entities.Customer;
import com.template.service.core.mapper.Mapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Configuration
public class CustomerBulkCreateTask {

    public static final TaskDescriptor<CustomerRequestDto> CUSTOMER_BULK_CREATE =
            TaskDescriptor.of("customer-bulk-create", CustomerRequestDto.class);

    private final AtomicInteger remainingErrors = new AtomicInteger(1);

    @Bean
    public OneTimeTask<CustomerRequestDto> customerBulkCreateTask(Mapper mapper) {
        return Tasks.oneTime(CUSTOMER_BULK_CREATE)
                .execute((instance, ctx) -> processItem(instance.getData(), mapper));
    }

    // Pins the starter to Jackson 2, matching the project's Jackson stack instead of its Jackson 3 default.
    @Bean
    public DbSchedulerCustomizer customerBulkSerializerCustomizer() {
        return new DbSchedulerCustomizer() {
            @Override
            public Optional<Serializer> serializer() {
                return Optional.of(new JacksonSerializer());
            }
        };
    }

    private void processItem(CustomerRequestDto dto, Mapper mapper) {
        log.info("Processing processed dto (create): {}", dto.getId());
        var customer = mapper.mapToCreate(dto, new Customer());
        if (remainingErrors.decrementAndGet() == 0) {
            log.warn("Simulating exception for item: {}", dto.getId());
            throw new InternalServerErrorException("Simulated conflict error for dto " + dto.getId());
        }

        log.info("Mapped customer: {}", customer);
    }
}
