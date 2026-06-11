package com.template.api.http_errors;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiFieldError(
        Integer status,
        String rejectedValue,
        String property,
        String path,
        String code,
        String message
) {
    public ApiFieldError(String rejectedValue, String property, String path, String code, String message) {
        this(null, rejectedValue, property, path, code, message);
    }

    public ApiFieldError(Integer status, String code, String message) {
        this(status, null, null, null, code, message);
    }
}