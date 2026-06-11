package com.template.api.dtos.api_key;

import com.template.data.entities.core.ApiKey;

import java.time.Instant;

public record ApiKeyResponse(
        Long id,
        String prefixHint,
        String subject,
        String label,
        Instant createdAt,
        Instant expiresAt,
        Instant revokedAt,
        Instant lastUsedAt) {

    public static ApiKeyResponse from(ApiKey apiKey) {
        return new ApiKeyResponse(
                apiKey.getId(),
                apiKey.getPrefixHint(),
                apiKey.getSubject(),
                apiKey.getLabel(),
                apiKey.getCreatedAt(),
                apiKey.getExpiresAt(),
                apiKey.getRevokedAt(),
                apiKey.getLastUsedAt()
        );
    }
}
