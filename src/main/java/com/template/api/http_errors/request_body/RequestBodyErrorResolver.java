package com.template.api.http_errors.request_body;

import com.template.api.dtos.core.ApiResult;
import com.template.api.http_errors.ApiErrorType;
import com.template.api.http_errors.ApiFieldError;
import com.template.api.http_errors.request_body.capture.RequestBodyReader;
import com.template.api.http_errors.request_body.parsing.JsonTokenScanner;
import com.template.api.http_errors.request_body.parsing.ScanResult;
import com.template.api.http_errors.request_body.processing.JacksonErrorMapper;
import com.template.api.http_errors.request_body.processing.SafeBodyProcessor;
import com.template.service.core.operations.log.RequestLogService;
import com.template.service.core.operations.route.ValidationRouteRegistry;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class RequestBodyErrorResolver {

    private static final String NOT_NULL_CODE = "VALIDATION_ERROR_NOT_NULL";
    private static final String PARSE_CODE = "JSON_PARSE_ERROR";

    private final RequestBodyReader bodyReader;
    private final ValidationRouteRegistry routeRegistry;
    private final RequestLogService requestLogService;
    private final SafeBodyProcessor safeBodyProcessor;

    public ApiResult<?> resolve(HttpMessageNotReadableException ex, HttpServletRequest request, Class<?> targetType) {
        var originalBody = bodyReader.read();
        var scanResult = JsonTokenScanner.scan(originalBody);
        var route = routeRegistry.match(request.getMethod(), request.getRequestURI(), scanResult.safeBody());
        var validationErrors = route != null
                ? safeBodyProcessor.collectErrors(scanResult.safeBody(), route)
                : safeBodyProcessor.collectErrors(scanResult.safeBody(), targetType);
        var allErrors = deduplicate(mergeErrors(ex, scanResult, validationErrors));
        return buildResponse(allErrors, request, scanResult.safeBody());
    }

    private List<ApiFieldError> mergeErrors(HttpMessageNotReadableException ex, ScanResult scanResult,
                                            List<ApiFieldError> validationErrors) {
        boolean usedFallback = !scanResult.hasErrors();
        if (usedFallback && !validationErrors.isEmpty()) return new ArrayList<>(validationErrors);
        var errors = new ArrayList<>(scanResult.errors());
        if (usedFallback) errors.add(JacksonErrorMapper.mapUnreadableException(ex));
        errors.addAll(validationErrors);
        return errors;
    }

    private List<ApiFieldError> deduplicate(List<ApiFieldError> errors) {
        var distinct = errors.stream().distinct().toList();
        var parseKeys = distinct.stream()
                .filter(e -> PARSE_CODE.equals(e.code()))
                .map(e -> fieldKey(e.path(), e.property()))
                .collect(Collectors.toSet());
        var keysWithOtherErrors = new HashSet<String>();
        distinct.stream()
                .filter(e -> !NOT_NULL_CODE.equals(e.code()))
                .forEach(e -> keysWithOtherErrors.add(fieldKey(e.path(), e.property())));
        return distinct.stream()
                .filter(e -> !isValidationArtifact(e, parseKeys))
                .filter(e -> !NOT_NULL_CODE.equals(e.code()) || !keysWithOtherErrors.contains(fieldKey(e.path(), e.property())))
                .toList();
    }

    private boolean isValidationArtifact(ApiFieldError error, Set<String> parseKeys) {
        return error.code() != null
                && error.code().startsWith("VALIDATION_ERROR_")
                && parseKeys.contains(fieldKey(error.path(), error.property()));
    }

    private String fieldKey(String path, String property) {
        if (path == null) return property;
        int end = path.indexOf(']');
        var prefix = (path.startsWith("[") && end >= 0) ? path.substring(0, end + 1) : "";
        return prefix + (property != null ? property : "");
    }

    private ApiResult<?> buildResponse(List<ApiFieldError> errors, HttpServletRequest request, String safeBody) {
        if (hasServerError(errors)) return ApiResult.error(ApiErrorType.INTERNAL_SERVER_ERROR);
        ApiResult<?> response = isSingleMode(safeBody)
                ? ApiResult.errors(ApiErrorType.BAD_REQUEST, errors)
                : safeBodyProcessor.bulkResponse(errors, safeBody);
        requestLogService.log(request, response);
        return response;
    }

    private boolean hasServerError(List<ApiFieldError> errors) {
        return errors.stream().anyMatch(e -> e.code() != null && e.code().contains("INTERNAL_SERVER_ERROR"));
    }

    private boolean isSingleMode(String safeBody) {
        return safeBody == null || !safeBody.stripLeading().startsWith("[");
    }
}
