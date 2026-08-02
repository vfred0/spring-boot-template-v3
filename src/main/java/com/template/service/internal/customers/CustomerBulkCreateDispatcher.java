package com.template.service.internal.customers;

import com.github.kagkarlsson.scheduler.Scheduler;
import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask;
import com.template.api.dtos.customer.CustomerRequestDto;
import org.springframework.stereotype.Service;

@Service
public class CustomerBulkCreateDispatcher {

    private final Scheduler scheduler;
    private final OneTimeTask<CustomerRequestDto> customerBulkCreateTask;

    public CustomerBulkCreateDispatcher(Scheduler scheduler, OneTimeTask<CustomerRequestDto> customerBulkCreateTask) {
        this.scheduler = scheduler;
        this.customerBulkCreateTask = customerBulkCreateTask;
    }

    public void schedule(CustomerRequestDto dto) {
        scheduler.scheduleIfNotExists(customerBulkCreateTask.schedulableInstance(dto.getId(), dto));
    }
}
