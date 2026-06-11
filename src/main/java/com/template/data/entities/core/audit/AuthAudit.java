package com.template.data.entities.core.audit;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "auth_audit")
@Getter
@Setter
public class AuthAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    private String subject;

    @Column(name = "api_key_id")
    private Long apiKeyId;

    @Column(name = "client_ip")
    private String clientIp;

    @Column(name = "user_agent")
    private String userAgent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuthAuditOutcome outcome;

    @Column(name = "request_path")
    private String requestPath;

    @Column(name = "request_method")
    private String requestMethod;
}
