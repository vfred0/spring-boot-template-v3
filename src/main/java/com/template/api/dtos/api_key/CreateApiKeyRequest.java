package com.template.api.dtos.api_key;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record CreateApiKeyRequest(

        @NotBlank
        @Size(max = 255)
        @Schema(example = "service-account-ci")
        String subject,

        @NotBlank
        @Size(max = 100)
        @Schema(example = "CI pipeline key")
        String label,

        @Size(max = 512)
        @Schema(example = "10.0.0.0/8,192.168.1.5/32")
        String allowedIps,

        @Schema(example = "2027-01-01T00:00:00Z")
        Instant expiresAt
) {}
