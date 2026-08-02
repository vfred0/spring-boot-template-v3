package com.template.api.integrationtest;

import com.github.kagkarlsson.scheduler.Scheduler;
import com.github.kagkarlsson.scheduler.ScheduledExecution;
import com.template.MainApplication;
import com.template.api.dtos.customer.AddressDto;
import com.template.api.dtos.customer.CustomerRequestDto;
import com.template.api.dtos.customer.CustomerStatus;
import com.template.api.util.AbstractIntegrationTest;
import com.template.config.keycloak.KeycloakProperties;
import com.template.config.security.RateLimitingFilter;
import com.template.service.internal.customers.CustomerBulkService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import static com.template.service.internal.customers.CustomerBulkCreateTask.CUSTOMER_BULK_CREATE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = MainApplication.class)
class CustomerBulkCreateTaskIT extends AbstractIntegrationTest {

    private final CustomerBulkService customerBulkService;
    private final Scheduler scheduler;

    CustomerBulkCreateTaskIT(@Qualifier("keycloakProperties") KeycloakProperties props,
                              CacheManager cacheManager,
                              RateLimitingFilter rateLimitingFilter,
                              CustomerBulkService customerBulkService,
                              Scheduler scheduler) {
        super(props, cacheManager, rateLimitingFilter);
        this.customerBulkService = customerBulkService;
        this.scheduler = scheduler;
    }

    @Test
    void bulkCreate_persistsDurableTaskAndSchedulerExecutesIt() throws Exception {
        String customerId = "it-customer-" + Instant.now().toEpochMilli();
        CustomerRequestDto dto = buildValidDto(customerId);

        var result = customerBulkService.create(Set.of(dto)).get();
        assertThat(result.isSuccess()).isTrue();

        var taskInstanceId = CUSTOMER_BULK_CREATE.instanceId(customerId);
        Optional<ScheduledExecution<Object>> afterSchedule = scheduler.getScheduledExecution(taskInstanceId);
        assertThat(afterSchedule).as("Task row should be persisted right after scheduling").isPresent();

        awaitTaskRan(taskInstanceId);
    }

    private void awaitTaskRan(com.github.kagkarlsson.scheduler.task.TaskInstanceId taskInstanceId) throws InterruptedException {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(20));
        while (Instant.now().isBefore(deadline)) {
            if (hasCompletedOrFailedAtLeastOnce(taskInstanceId)) {
                return;
            }
            Thread.sleep(200);
        }
        fail("db-scheduler did not execute the scheduled task within the expected time");
    }

    private boolean hasCompletedOrFailedAtLeastOnce(com.github.kagkarlsson.scheduler.task.TaskInstanceId taskInstanceId) {
        Optional<ScheduledExecution<Object>> current = scheduler.getScheduledExecution(taskInstanceId);
        return current.isEmpty() || current.get().getLastFailure() != null;
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
