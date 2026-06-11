package com.template.api.resources.customers;


import com.template.api.dtos.customer.CustomerPatchDto;
import com.template.api.dtos.customer.CustomerRequestDto;
import com.template.api.resources.core.BulkResource;
import com.template.config.api_version.ApiVersion;
import com.template.service.core.operations.log.RequestLogService;
import com.template.service.core.operations.route.ValidationRouteRegistry;
import com.template.service.internal.customers.CustomerBulkService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/customers", version = ApiVersion.V1)
public class CustomerBulkResource extends BulkResource<CustomerRequestDto, CustomerPatchDto> {

    public CustomerBulkResource(CustomerBulkService service, RequestLogService requestLogService,
                                ValidationRouteRegistry registry) {
        super(service, requestLogService, registry, CustomerRequestDto.class, CustomerPatchDto.class);
    }
}
