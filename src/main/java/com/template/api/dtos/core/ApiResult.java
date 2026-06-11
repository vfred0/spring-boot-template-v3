package com.template.api.dtos.core;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.template.api.http_errors.ApiErrorType;
import com.template.api.http_errors.ApiFieldError;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResult<T>(
        Integer status,
        String code,
        String message,
        T data,
        List<ApiFieldError> errors,
        Summary summary
) {
    public record Summary(int total, int processed, int failed) {}

    public static <T> ApiResult<T> ok(T data) {
        return new ApiResult<>(200, "OK", null, data, null, null);
    }

    public static <T> ApiResult<T> ok(T data, String message) {
        return new ApiResult<>(200, "OK", message, data, null, null);
    }

    public static <T> ApiResult<T> created(T data) {
        return new ApiResult<>(201, "CREATED", null, data, null, null);
    }

    public static <T> ApiResult<T> error(ApiErrorType type) {
        return error(type, type.message());
    }

    public static <T> ApiResult<T> error(ApiErrorType type, String message) {
        return new ApiResult<>(type.status().value(), type.code(),
                message != null ? message : type.message(), null, null, null);
    }

    public static ApiResult<Void> errors(ApiErrorType type, List<ApiFieldError> errors) {
        return new ApiResult<>(type.status().value(), type.code(), type.message(), null,
                errors != null ? List.copyOf(errors) : null, null);
    }

    public static ApiResult<Void> bulk(List<ApiFieldError> errors, int processed, int failed) {
        var summary = new Summary(processed + failed, processed, failed);
        var type = (processed > 0 && failed > 0) ? ApiErrorType.MULTI_STATUS : ApiErrorType.BAD_REQUEST;
        var normalized = normalizeStatuses(errors, type);
        return new ApiResult<>(type.status().value(), type.code(), type.message(), null, normalized, summary);
    }

    private static List<ApiFieldError> normalizeStatuses(List<ApiFieldError> errors, ApiErrorType type) {
        if (errors == null) return null;
        boolean multiStatus = type == ApiErrorType.MULTI_STATUS;
        Integer fallback = multiStatus ? ApiErrorType.BAD_REQUEST.status().value() : null;
        return errors.stream()
                .map(e -> multiStatus == (e.status() == null)
                        ? new ApiFieldError(fallback, e.rejectedValue(), e.property(), e.path(), e.code(), e.message())
                        : e)
                .toList();
    }
}
