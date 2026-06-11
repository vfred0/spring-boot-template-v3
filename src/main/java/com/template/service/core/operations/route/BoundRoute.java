package com.template.service.core.operations.route;

import java.util.Set;
import java.util.function.Function;

public record BoundRoute(
        String method,
        String uriSubstring,
        Class<?> targetDtoClass,
        Set<RouteMode> supportedModes,
        Function<Set<Object>, RouteResult> processor
) implements ValidationRoute {

    @Override
    public RouteResult process(Set<Object> items, RouteMode mode) {
        return processor.apply(items);
    }

    @Override
    public String getMethod() { return method; }

    @Override
    public String getUriSubstring() { return uriSubstring; }

    @Override
    public Class<?> getTargetDtoClass() { return targetDtoClass; }

    @Override
    public Set<RouteMode> supportedModes() { return supportedModes; }
}
