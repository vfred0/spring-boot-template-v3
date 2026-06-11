package com.template.service.core.operations.route;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ValidationRouteRegistry {

    private final List<ValidationRoute> routes = new ArrayList<>();

    public void register(ValidationRoute... values) {
        routes.addAll(List.of(values));
    }

    public ValidationRoute match(String method, String uri, String body) {
        boolean isArray = body != null && body.stripLeading().startsWith("[");
        RouteMode requestedMode = isArray ? RouteMode.BULK : RouteMode.SINGLE;
        return routes.stream()
                .filter(r -> r.matches(method, uri))
                .filter(r -> r.supportedModes().contains(requestedMode))
                .findFirst()
                .orElse(null);
    }

    public ValidationRoute match(String method, String uri) {
        return routes.stream()
                .filter(r -> r.matches(method, uri))
                .findFirst()
                .orElse(null);
    }
}
