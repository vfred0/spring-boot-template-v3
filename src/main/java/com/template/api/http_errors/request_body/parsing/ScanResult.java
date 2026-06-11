package com.template.api.http_errors.request_body.parsing;

import com.template.api.http_errors.ApiFieldError;

import java.util.List;

public record ScanResult(List<ApiFieldError> errors, String safeBody) {

    public boolean hasErrors() {
        return !errors.isEmpty();
    }
}
