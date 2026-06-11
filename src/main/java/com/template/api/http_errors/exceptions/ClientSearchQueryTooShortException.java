package com.template.api.http_errors.exceptions;

import lombok.Getter;

@Getter
public class ClientSearchQueryTooShortException extends RuntimeException {

    private final int minLength;

    public ClientSearchQueryTooShortException(int minLength) {
        super();
        this.minLength = minLength;
    }

    public String getMessageCode() {
        return "error.client.searchQueryTooShort";
    }
}

