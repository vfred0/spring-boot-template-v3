package com.template.api.http_errors;

import org.springframework.http.HttpStatus;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public enum ApiErrorType {
    UNKNOWN_ERROR(HttpStatus.BAD_REQUEST, "UNKNOWN", "Unknown error processing JSON request"),
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "The request could not be processed due to validation errors"),
    MULTI_STATUS(HttpStatus.MULTI_STATUS, "MULTI_STATUS", "The request was processed with some errors"),
    MISSING_API_KEY(HttpStatus.UNAUTHORIZED, "MISSING_API_KEY", "Missing required API-KEY header (X-API-KEY)"),
    INVALID_API_KEY(HttpStatus.FORBIDDEN, "INVALID_API_KEY", "The provided API-KEY is invalid"),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "The API-KEY is valid but does not have access to this resource"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authentication failed"),
    INVALID_GRANT(HttpStatus.UNAUTHORIZED, "INVALID_GRANT", "The provided credentials or grant are invalid"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "The provided token is invalid"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "FORBIDDEN", "You do not have permission to access this resource"),
    CONFLICT(HttpStatus.CONFLICT, "CONFLICT", "The request conflicts with the current state of the resource"),
    TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "TOO_MANY_REQUESTS", "Too many requests. Please try again later."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "NOT_FOUND", "The requested resource was not found"),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "An unexpected error occurred. Please try again later."),
    SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", "The service is temporarily unavailable. Please try again later."),
    GATEWAY_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "GATEWAY_TIMEOUT", "The upstream service did not respond in time."),
    INVALID_API_VERSION(HttpStatus.BAD_REQUEST, "INVALID_API_VERSION", "The API version specified is not supported"),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", "Not allowed method");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ApiErrorType(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    public HttpStatus status() {
        return status;
    }

    public String code() {
        return code;
    }

    public String message() {
        return message;
    }

    public ApiFieldError toApiFieldError() {
        return new ApiFieldError(status.value(), code, message);
    }

    public ApiFieldError toApiFieldError(String detail) {
        if (detail == null || detail.isBlank()) {
            return toApiFieldError();
        }
        return new ApiFieldError(status.value(), code, message + ": " + detail);
    }

    public String titleKey() {
        return "api.error.title." + camelKey();
    }

    public String messageKey() {
        return "api.error." + camelKey();
    }

    private String camelKey() {
        String[] parts = code.toLowerCase(Locale.ROOT).split("_");
        if (parts.length == 1) return parts[0];
        StringBuilder sb = new StringBuilder(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            sb.append(Character.toUpperCase(parts[i].charAt(0)));
            sb.append(parts[i].substring(1));
        }
        return sb.toString();
    }

    public Map<String, Object> toMethodNotAllowedBody(String method, String path) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", "Not allowed method [" + method + "] to " + path);
        return body;
    }
}
