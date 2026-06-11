package com.template.api.resources.core;

import com.template.service.core.operations.bulk.BulkService;
import com.template.service.core.operations.log.RequestLogService;
import com.template.service.core.operations.route.*;
import com.template.service.core.operations.route.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public abstract class BulkResource<C, U> extends BaseResource<C, U> {

    private final BulkService<C, U> service;

    protected BulkResource(BulkService<C, U> service, RequestLogService requestLogService,
                           ValidationRouteRegistry registry, Class<C> createDtoClass, Class<U> patchDtoClass) {
        super(requestLogService, registry, createDtoClass, patchDtoClass);
        this.service = service;
    }

    @Override
    protected ValidationRoute[] buildRoutes(String uri) {
        return new ValidationRoute[]{
                new BoundRoute("POST", uri, createDtoClass, EnumSet.of(RouteMode.BULK),
                        items -> new RouteResult.Bulk(service.create(castItems(items, createDtoClass)))),
                new BoundRoute("PATCH", uri, patchDtoClass, EnumSet.of(RouteMode.BULK),
                        items -> new RouteResult.Bulk(service.patch(castItems(items, patchDtoClass))))
        };
    }

    @PostMapping
    public CompletableFuture<ResponseEntity<?>> create(@RequestBody Set<C> dtos, HttpServletRequest request) {
        return service.create(dtos).thenApply(result -> {
            if (result.isFailure()) {
                var error = result.getErrorOrNull();
                requestLogService.log(request, error);
                return ResponseEntity.status(error.status()).body(error);
            }
            return ResponseEntity.status(HttpStatus.CREATED).build();
        });
    }

    @PatchMapping
    public CompletableFuture<ResponseEntity<?>> patch(@RequestBody Set<U> dtos, HttpServletRequest request) {
        return service.patch(dtos).thenApply(result -> {
            if (result.isFailure()) {
                var error = result.getErrorOrNull();
                requestLogService.log(request, error);
                return ResponseEntity.status(error.status()).body(error);
            }
            return ResponseEntity.status(HttpStatus.ACCEPTED).build();
        });
    }

    private <T> Set<T> castItems(Set<Object> items, Class<T> clazz) {
        Set<T> result = new LinkedHashSet<>();
        items.forEach(o -> result.add(clazz.cast(o)));
        return result;
    }
}
