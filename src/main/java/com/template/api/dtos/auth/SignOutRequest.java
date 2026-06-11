package com.template.api.dtos.auth;

import jakarta.validation.constraints.NotBlank;

public record SignOutRequest(
        @NotBlank(message = "{validation.clientId.required}")
        String clientId,
        @NotBlank(message = "{validation.clientSecret.required}")
        String clientSecret
) {}
