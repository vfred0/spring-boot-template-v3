package com.template.api.http_errors.exceptions;

import lombok.Getter;

@Getter
public class ApiKeyNotFoundException extends RuntimeException {
    private final Long apiKeyId;

    public ApiKeyNotFoundException(Long id) {
        super("API key with id=" + id + " not found");
        this.apiKeyId = id;
    }

    public String getMessageCode() {
        return "error.apiKey.notFound";
    }
}
