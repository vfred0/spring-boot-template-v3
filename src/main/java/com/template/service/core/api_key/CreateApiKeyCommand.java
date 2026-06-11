package com.template.service.core.api_key;

import java.time.Instant;

public record CreateApiKeyCommand(
        String subject,
        String label,
        String allowedIps,
        Instant expiresAt) {
}
