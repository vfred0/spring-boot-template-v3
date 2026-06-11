package com.template.api.http_errors.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class InternalServerErrorException extends RuntimeException {
    public InternalServerErrorException(MessageException messageException) {
        super(messageException.getMessage());
    }

    public InternalServerErrorException(String detail) {
        super(detail);
    }

    public InternalServerErrorException() {
        super(MessageException.INTERNAL_SERVER_ERROR.getMessage());
    }
}