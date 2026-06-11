package com.template.api.http_errors.request_body.processing;

import com.template.api.dtos.core.ApiResult;
import com.template.api.http_errors.ApiErrorType;
import com.template.api.http_errors.ApiFieldError;
import com.template.service.core.operations.route.RouteMode;
import com.template.service.core.operations.route.RouteResult;
import com.template.service.core.operations.route.ValidationRoute;
import com.template.service.core.operations.validation.ValidationMapper;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class SafeBodyProcessor {

    private final ObjectMapper objectMapper;
    private final Validator validator;

    public List<ApiFieldError> collectErrors(String safeBody, ValidationRoute route) {
        if (route == null || safeBody == null || safeBody.isBlank()) return List.of();
        try {
            var root = objectMapper.readTree(safeBody);
            if (root.isArray()) return collectBulkErrors(root, route);
            return collectSingleErrors(root, route);
        } catch (Exception e) {
            log.debug("Safe body processing failed", e);
        }
        return List.of();
    }

    public List<ApiFieldError> collectErrors(String safeBody, Class<?> targetType) {
        if (targetType == null || safeBody == null || safeBody.isBlank()) return List.of();
        try {
            var root = objectMapper.readTree(safeBody);
            if (root.isArray()) return List.of();
            var result = deserializeNode(root, targetType, -1);
            var errors = new ArrayList<>(result.errors());
            if (result.dto() != null) errors.addAll(validate(result.dto()));
            return errors;
        } catch (Exception e) {
            log.debug("Safe body processing failed", e);
        }
        return List.of();
    }

    private List<ApiFieldError> validate(Object dto) {
        var violations = validator.validate(dto);
        return violations.isEmpty() ? List.of() : ValidationMapper.mapViolations(violations, -1);
    }

    public ApiResult<Void> bulkResponse(List<ApiFieldError> errors, String safeBody) {
        int total = countItems(safeBody);
        if (total <= 0) return ApiResult.errors(ApiErrorType.BAD_REQUEST, errors);
        int failed = (int) countDistinctErroredItems(errors);
        return ApiResult.bulk(errors, total - failed, failed);
    }

    private int countItems(String safeBody) {
        try {
            var root = objectMapper.readTree(safeBody);
            return (root != null && root.isArray()) ? root.size() : 0;
        } catch (Exception ignored) {
            return 0;
        }
    }

    private List<ApiFieldError> collectBulkErrors(JsonNode root, ValidationRoute route) {
        var errors = new ArrayList<ApiFieldError>();
        List<Object> dtos = new ArrayList<>(Collections.nCopies(root.size(), null));
        for (int i = 0; i < root.size(); i++) {
            var result = deserializeNode(root.get(i), route.getTargetDtoClass(), i);
            errors.addAll(result.errors());
            if (result.dto() != null) dtos.set(i, result.dto());
        }
        errors.addAll(processValidBulkItems(dtos, route));
        return errors;
    }

    private List<ApiFieldError> collectSingleErrors(JsonNode root, ValidationRoute route) {
        var result = deserializeNode(root, route.getTargetDtoClass(), -1);
        var errors = new ArrayList<>(result.errors());
        if (result.dto() != null) errors.addAll(processSingleItem(result.dto(), route));
        return errors;
    }

    private List<ApiFieldError> processSingleItem(Object dto, ValidationRoute route) {
        var routeResult = route.process(Set.of(dto), RouteMode.SINGLE);
        if (routeResult instanceof RouteResult.Single(var res) && res.isFailure()) {
            return new ArrayList<>(res.getErrorOrNull().errors());
        }
        return List.of();
    }

    private List<ApiFieldError> processValidBulkItems(List<Object> dtos, ValidationRoute route) {
        var validSet = new LinkedHashSet<>(dtos.stream().filter(Objects::nonNull).toList());
        if (validSet.isEmpty()) return List.of();
        try {
            var routeResult = route.process(validSet, RouteMode.BULK);
            if (routeResult instanceof RouteResult.Bulk bulk) {
                var result = bulk.future().join();
                if (result.isFailure()) return new ArrayList<>(result.getErrorOrNull().errors());
            }
        } catch (Exception e) {
            log.debug("Batch processing failed during safe body validation", e);
        }
        return List.of();
    }

    private DeserializationResult deserializeNode(JsonNode node, Class<?> targetType, int index) {
        var errors = new ArrayList<ApiFieldError>();
        var workingNode = node.deepCopy();
        int maxRetries = workingNode.isObject() ? workingNode.size() : 0;

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                return new DeserializationResult(objectMapper.treeToValue(workingNode, targetType), errors);
            } catch (JacksonException jpe) {
                errors.add(JacksonErrorMapper.mapException(jpe, index));
                var propName = extractFailedProperty(jpe);
                if (propName != null && workingNode instanceof ObjectNode obj) obj.putNull(propName);
                else break;
            } catch (Exception e) {
                log.debug("Unexpected error deserializing item at index {}", index, e);
                break;
            }
        }
        return new DeserializationResult(null, errors);
    }

    private String extractFailedProperty(JacksonException jpe) {
        var refs = jpe.getPath();
        if (refs == null || refs.isEmpty()) return null;
        return refs.get(refs.size() - 1).getPropertyName();
    }

    private long countDistinctErroredItems(List<ApiFieldError> errors) {
        return errors.stream()
                .map(ApiFieldError::path)
                .filter(p -> p != null && p.contains("["))
                .map(p -> p.substring(p.indexOf("["), p.indexOf("]", p.indexOf("[")) + 1))
                .distinct()
                .count();
    }

    private record DeserializationResult(Object dto, List<ApiFieldError> errors) {}
}
