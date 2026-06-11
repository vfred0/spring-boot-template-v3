package com.template.api.dtos.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(
        @NotBlank(message = "{validation.clientId.required}")
        @Schema(example = "spring-app")
        String clientId,
        @NotBlank(message = "{validation.clientSecret.required}")
        @Schema(example = "spring-app-secret")
        String clientSecret
) {}
