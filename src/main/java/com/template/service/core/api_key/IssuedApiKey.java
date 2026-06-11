package com.template.service.core.api_key;

public record IssuedApiKey(
        Long id,
        String rawKey,
        String prefixHint) {
}
