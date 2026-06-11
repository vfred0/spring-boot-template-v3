package com.template.service.core.operations.route;

import java.util.EnumSet;
import java.util.Set;

public interface ValidationRoute {

    String getMethod();
    String getUriSubstring();
    Class<?> getTargetDtoClass();

    RouteResult process(Set<Object> items, RouteMode mode);

    default Set<RouteMode> supportedModes() {
        return EnumSet.allOf(RouteMode.class);
    }

    default boolean matches(String method, String uri) {
        if (method == null || uri == null) return false;
        return getMethod().equalsIgnoreCase(method) && uri.contains(getUriSubstring());
    }
}
