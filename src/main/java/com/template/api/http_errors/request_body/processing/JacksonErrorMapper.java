package com.template.api.http_errors.request_body.processing;

import com.template.api.http_errors.ApiErrorType;
import com.template.api.http_errors.ApiFieldError;
import com.template.api.http_errors.exceptions.InvalidEnumValueException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import tools.jackson.core.JacksonException;

import java.util.List;

public final class JacksonErrorMapper {

    private static final String JSON_ERROR = "JSON_ERROR";
    private static final String JSON_PARSE_ERROR = "JSON_PARSE_ERROR";
    private static final String ENUM_VALIDATION_ERROR = "VALIDATION_ERROR_ENUM_VALUE";

    private JacksonErrorMapper() {}

    public static ApiFieldError mapException(JacksonException jpe, int itemIndex) {
        var cause = jpe.getCause();
        var property = extractPropertyName(jpe.getPath());
        var path = buildPath(jpe.getPath(), itemIndex);

        if (cause instanceof InvalidEnumValueException inv) {
            return new ApiFieldError(inv.getRejectedValue(), property, path, inv.getCode(), inv.getMessage());
        }
        if (cause != null && cause.getMessage() != null && !cause.getMessage().isBlank()) {
            return new ApiFieldError(null, property, path, resolveCode(cause), cause.getMessage());
        }
        return new ApiFieldError(null, property, path, JSON_ERROR, jpe.getOriginalMessage());
    }

    public static ApiFieldError mapUnreadableException(HttpMessageNotReadableException ex) {
        if (ex.getCause() instanceof JacksonException jpe) return mapException(jpe, -1);
        return new ApiFieldError(
                ApiErrorType.UNKNOWN_ERROR.code(), "body", "", JSON_ERROR, ApiErrorType.UNKNOWN_ERROR.message()
        );
    }

    private static String extractPropertyName(List<JacksonException.Reference> refs) {
        if (refs == null || refs.isEmpty()) return null;
        return refs.get(refs.size() - 1).getPropertyName();
    }

    private static String buildPath(List<JacksonException.Reference> refs, int itemIndex) {
        var sb = new StringBuilder();
        if (itemIndex >= 0) sb.append("[%d]".formatted(itemIndex));
        for (var ref : refs) {
            if (ref.getPropertyName() != null) {
                if (!sb.isEmpty() && !sb.toString().endsWith("]")) sb.append(".");
                sb.append(ref.getPropertyName());
            } else if (ref.getIndex() >= 0) {
                sb.append("[%d]".formatted(ref.getIndex()));
            }
        }
        return sb.toString();
    }

    private static String resolveCode(Throwable cause) {
        if (cause.getMessage().toLowerCase().contains("enum")) return ENUM_VALIDATION_ERROR;
        return JSON_PARSE_ERROR;
    }
}
