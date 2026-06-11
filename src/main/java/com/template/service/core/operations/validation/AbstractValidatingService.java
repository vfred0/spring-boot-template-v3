package com.template.service.core.operations.validation;

import com.template.api.http_errors.ApiFieldError;
import com.template.api.http_errors.exceptions.BulkItemProcessingException;
import com.template.api.http_errors.exceptions.InvalidEnumValueException;
import com.template.service.core.operations.Result;
import com.template.service.core.operations.route.ValidationRouteRegistry;
import jakarta.validation.Validator;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

public abstract class AbstractValidatingService {

    protected final Validator validator;
    private final ValidationRouteRegistry routeRegistry;

    protected AbstractValidatingService(Validator validator, ValidationRouteRegistry routeRegistry) {
        this.validator = validator;
        this.routeRegistry = routeRegistry;
    }

    protected <T> Result<T, List<ApiFieldError>> validateItem(T item, int index) {
        var violations = validator.validate(item);
        return violations.isEmpty()
                ? Result.success(item)
                : Result.failure(ValidationMapper.mapViolations(violations, index));
    }

    protected void handleProcessingException(BulkItemProcessingException bipe, List<ApiFieldError> errors) {

        RuntimeException cause = bipe.getException();

        String rejectedValue = (cause instanceof InvalidEnumValueException inv)
                ? inv.getRejectedValue()
                : bipe.getRejectedValue();

        errors.add(new ApiFieldError(
                extractStatus(cause),
                rejectedValue,
                bipe.getProperty(),
                bipe.getPath(),
                errorCode(cause),
                messageOf(cause)
        ));
    }

    protected String resolveRouteInfo() {
        try {
            var attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return "UNKNOWN RESOURCE";
            var req = attrs.getRequest();
            var route = routeRegistry.match(req.getMethod(), req.getRequestURI());
            return route != null
                    ? route.getMethod() + " " + route.getUriSubstring()
                    : req.getMethod() + " " + req.getRequestURI();
        } catch (Exception ignored) {
            return "UNKNOWN RESOURCE";
        }
    }

    protected int extractStatus(Throwable ex) {
        var rs = AnnotationUtils.findAnnotation(ex.getClass(), ResponseStatus.class);
        return rs != null ? rs.value().value() : 500;
    }

    protected String errorCode(Throwable ex) {
        var rs = AnnotationUtils.findAnnotation(ex.getClass(), ResponseStatus.class);
        return rs != null ? rs.value().name() : "INTERNAL_SERVER";
    }

    protected String messageOf(Throwable ex) {
        return ex.getMessage() != null ? ex.getMessage() : "Error occurred during item processing";
    }
}
