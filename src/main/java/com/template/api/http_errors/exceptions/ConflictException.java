package com.template.api.http_errors.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class ConflictException extends RuntimeException {
    public ConflictException(MessageException messageException) {
        super(messageException.getMessage());
    }

    public ConflictException(String detail) {
        super(detail);
    }
}