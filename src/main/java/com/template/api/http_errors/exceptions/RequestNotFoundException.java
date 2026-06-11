package com.template.api.http_errors.exceptions;

import lombok.Getter;

import java.util.UUID;

@Getter
public class RequestNotFoundException extends RuntimeException {
    private final UUID requestId;

    public RequestNotFoundException(UUID requestId) {
        super("Request with id=" + requestId + " not found");
        this.requestId = requestId;
    }

    public String getMessageCode() {
        return "error.request.notFound";
    }
}

