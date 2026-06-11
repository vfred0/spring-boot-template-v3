package com.template.api.resources;


import com.template.api.dtos.customer.AddressDto;
import com.template.api.resources.core.BulkResource;
import com.template.config.api_version.ApiVersion;
import com.template.service.core.operations.log.RequestLogService;
import com.template.service.core.operations.route.ValidationRouteRegistry;
import com.template.service.internal.address.AddressService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/address", version = ApiVersion.V1)
public class AddressBulkResource extends BulkResource<AddressDto, AddressDto> {

    public AddressBulkResource(AddressService service, RequestLogService requestLogService,
                               ValidationRouteRegistry registry) {
        super(service, requestLogService, registry, AddressDto.class, AddressDto.class);
    }


    @PreAuthorize("hasRole('DEVELOPER')")
    @PostMapping(version = ApiVersion.V2)
    public ResponseEntity<?> create2(@RequestBody AddressDto dto) {
        return ResponseEntity.ok(dto);
    }
}