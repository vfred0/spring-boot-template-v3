package com.template.api.resources.core;

import com.template.service.core.operations.log.RequestLogService;
import com.template.service.core.operations.route.ValidationRoute;
import com.template.service.core.operations.route.ValidationRouteRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.web.bind.annotation.RequestMapping;


@RequiredArgsConstructor
public abstract class BaseResource<C, U> {

    protected final RequestLogService requestLogService;
    private final ValidationRouteRegistry registry;
    protected final Class<C> createDtoClass;
    protected final Class<U> patchDtoClass;

    @PostConstruct
    final void registerRoutes() {
        registry.register(buildRoutes(resolveUri()));
    }

    protected abstract ValidationRoute[] buildRoutes(String uri);

    private String resolveUri() {
        var mapping = AnnotationUtils.findAnnotation(getClass(), RequestMapping.class);
        if (mapping != null && mapping.path().length > 0) return mapping.path()[0];
        if (mapping != null && mapping.value().length > 0) return mapping.value()[0];
        throw new IllegalStateException(getClass().getSimpleName() + " must declare @RequestMapping with a path");
    }
}
