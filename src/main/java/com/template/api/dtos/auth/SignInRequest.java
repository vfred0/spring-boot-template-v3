package com.template.api.dtos.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record SignInRequest(
        @NotBlank(message = "{validation.username.required}")
        @Schema(example = "jdoe")
        String username,
        @NotBlank(message = "{validation.password.required}")
        @Schema(example = "P@ssw0rd!")
        String password
) {}
