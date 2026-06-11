package com.template.service.core.rbac;

import lombok.RequiredArgsConstructor;
import com.template.data.entities.core.audit.AuthAudit;
import com.template.data.daos.AuthAuditRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthAuditService {

    private final AuthAuditRepository authAuditRepository;

    @Async
    @Transactional
    public void record(AuthAuditEvent event) {
        AuthAudit audit = new AuthAudit();
        audit.setOccurredAt(Instant.now());
        audit.setSubject(event.subject());
        audit.setApiKeyId(event.apiKeyId());
        audit.setClientIp(event.clientIp());
        audit.setUserAgent(event.userAgent());
        audit.setOutcome(event.outcome());
        audit.setRequestPath(event.requestPath());
        audit.setRequestMethod(event.requestMethod());
        authAuditRepository.save(audit);
    }
}
