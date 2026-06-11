package com.template.service.core.operations.log;

import com.template.api.dtos.core.ApiResult;
import com.template.api.http_errors.ApiErrorType;
import com.template.data.daos.IRequestLogRepository;
import com.template.data.entities.core.RequestLog;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RequestLogService {

    private static final String VERSION_HEADER = "X-API-VERSION";
    private static final ApiErrorType UNKNOWN_ERROR = ApiErrorType.UNKNOWN_ERROR;

    private final IRequestLogRepository repository;
    private final ObjectMapper objectMapper;

    public void log(HttpServletRequest request, ApiResult<?> response) {
        if (request == null || response == null || response.status() == null) return;
        int status = response.status();
        if (status != 400 && status != 207) return;

        try {
            repository.save(RequestLog.builder()
                    .type(buildType(request))
                    .request(buildRequestLabel(request))
                    .response(serialize(response))
                    .build());
        } catch (RuntimeException ex) {
            log.warn("Failed to persist request log: {}", ex.getMessage());
        }
    }

    private String buildType(HttpServletRequest request) {
        String resource = extractResource(request.getRequestURI());
        return request.getMethod().toUpperCase() + "_" + toSnakeCase(resource).toUpperCase();
    }

    private String buildRequestLabel(HttpServletRequest request) {
        String version = normalizeVersion(request.getHeader(VERSION_HEADER));
        return "%s %s %s".formatted(request.getMethod(), request.getRequestURI(), version);
    }

    private String extractResource(String path) {
        int apiIndex = path != null ? path.indexOf("/api/") : -1;
        if (apiIndex < 0) return UNKNOWN_ERROR.code();
        String remainder = path.substring(apiIndex + 5);
        if (remainder.isBlank()) return UNKNOWN_ERROR.code();
        int slash = remainder.indexOf('/');
        return slash >= 0 ? remainder.substring(0, slash) : remainder;
    }

    private String toSnakeCase(String value) {
        if (value == null || value.isBlank()) return UNKNOWN_ERROR.code();
        String result = value
                .replaceAll("([a-z0-9])([A-Z])", "$1_$2")
                .replaceAll("[^A-Za-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        return result.isBlank() ? UNKNOWN_ERROR.code() : result;
    }

    private String normalizeVersion(String header) {
        if (header == null || header.isBlank()) return "V1";
        String v = header.trim();
        return "V" + (v.startsWith("V") || v.startsWith("v") ? v.substring(1) : v).split("\\.")[0];
    }

    private String serialize(ApiResult<?> response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            return "{\"status\":" + response.status() + ",\"message\":\"failed_to_serialize_response\"}";
        }
    }
}
