package com.template.api.resources.core;

import com.template.service.core.operations.log.RequestLogService;
import com.template.service.core.operations.route.*;
import com.template.service.core.operations.route.*;
import com.template.service.core.operations.single.SingleService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.EnumSet;
import java.util.Set;


public abstract class SingleResource<C, U> extends BaseResource<C, U> {

    private final SingleService<C, U> service;

    protected SingleResource(SingleService<C, U> service, RequestLogService requestLogService,
                             ValidationRouteRegistry registry, Class<C> createDtoClass, Class<U> patchDtoClass) {
        super(requestLogService, registry, createDtoClass, patchDtoClass);
        this.service = service;
    }

    @Override
    protected ValidationRoute[] buildRoutes(String uri) {
        return new ValidationRoute[]{
                new BoundRoute("POST", uri, createDtoClass, EnumSet.of(RouteMode.SINGLE),
                        items -> new RouteResult.Single(service.create(castSingle(items, createDtoClass)))),
                new BoundRoute("PATCH", uri, patchDtoClass, EnumSet.of(RouteMode.SINGLE),
                        items -> new RouteResult.Single(service.patch(castSingle(items, patchDtoClass))))
        };
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody C dto, HttpServletRequest request) {
        var result = service.create(dto);
        if (result.isFailure()) {
            var error = result.getErrorOrNull();
            requestLogService.log(request, error);
            return ResponseEntity.status(error.status()).body(error);
        }
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PatchMapping
    public ResponseEntity<?> patch(@RequestBody U dto, HttpServletRequest request) {
        var result = service.patch(dto);
        if (result.isFailure()) {
            var error = result.getErrorOrNull();
            requestLogService.log(request, error);
            return ResponseEntity.status(error.status()).body(error);
        }
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    private <T> T castSingle(Set<Object> items, Class<T> clazz) {
        return clazz.cast(items.iterator().next());
    }
}
