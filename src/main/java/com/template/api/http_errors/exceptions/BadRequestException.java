package com.template.api.http_errors.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class BadRequestException extends RuntimeException {
    public BadRequestException(MessageException messageException) {
        super(messageException.getMessage());
    }

    public BadRequestException(String detail) {
        super(detail);
    }

}
