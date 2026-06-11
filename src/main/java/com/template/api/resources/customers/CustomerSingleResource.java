package com.template.api.resources.customers;


import com.template.api.dtos.customer.CustomerPatchDto;
import com.template.api.dtos.customer.CustomerRequestDto;
import com.template.api.resources.core.SingleResource;
import com.template.config.api_version.ApiVersion;
import com.template.service.core.operations.log.RequestLogService;
import com.template.service.core.operations.route.ValidationRouteRegistry;
import com.template.service.internal.customers.CustomerService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/customers", version = ApiVersion.V2)
public class CustomerSingleResource extends SingleResource<CustomerRequestDto, CustomerPatchDto> {

    public CustomerSingleResource(CustomerService service, RequestLogService requestLogService,
                                  ValidationRouteRegistry registry) {
        super(service, requestLogService, registry, CustomerRequestDto.class, CustomerPatchDto.class);
    }
}
