package com.template.api.http_errors.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class EnumValueException extends RuntimeException {
    public EnumValueException(String message) {
        super(message);
    }
}
