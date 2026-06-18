package com.template.api.dtos.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record RegisterRequest(
        @NotBlank(message = "{validation.username.required}")
        @Schema(example = "jdoe")
        String username,
        @NotBlank(message = "{validation.password.required}")
        @Schema(example = "P@ssw0rd!")
        String password,
        @NotBlank(message = "{validation.firstName.required}")
        @Schema(example = "John")
        String firstName,
        @NotBlank(message = "{validation.lastName.required}")
        @Schema(example = "Doe")
        String lastName,
        @NotBlank(message = "{validation.email.required}")
        @Schema(example = "jdoe@example.com")
        String email
) {}
