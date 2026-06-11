package com.template.config.api_key;

import com.template.data.entities.core.audit.AuthAuditOutcome;
import org.springframework.security.core.Authentication;

public record ApiKeyAuthResult(
        Authentication authentication,
        AuthAuditOutcome outcome,
        Long apiKeyId,
        String subject) {

    public static ApiKeyAuthResult success(Authentication authentication, Long apiKeyId, String subject) {
        return new ApiKeyAuthResult(authentication, AuthAuditOutcome.SUCCESS, apiKeyId, subject);
    }

    public static ApiKeyAuthResult failure(AuthAuditOutcome outcome, Long apiKeyId, String subject) {
        return new ApiKeyAuthResult(null, outcome, apiKeyId, subject);
    }

    public boolean isSuccess() {
        return outcome == AuthAuditOutcome.SUCCESS;
    }
}
