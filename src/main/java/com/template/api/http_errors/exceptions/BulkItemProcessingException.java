package com.template.api.http_errors.exceptions;

import lombok.Getter;

@Getter
public class BulkItemProcessingException extends RuntimeException {
    private final String property;
    private final String rejectedValue;
    private final String path;
    private final RuntimeException exception;

    public BulkItemProcessingException(String property, String rejectedValue, String path, RuntimeException exception) {
        super(exception.getMessage(), exception);
        this.property = property;
        this.rejectedValue = rejectedValue;
        this.path = path;
        this.exception = exception;
    }
}
