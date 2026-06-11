package com.template.service.core.rbac;

import com.template.data.entities.core.audit.AuthAuditOutcome;

public record AuthAuditEvent(
        String subject,
        Long apiKeyId,
        String clientIp,
        String userAgent,
        AuthAuditOutcome outcome,
        String requestPath,
        String requestMethod) {
}
