package com.template.api.dtos.rbac;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateRoleRequest(

        @NotBlank
        @Size(max = 100)
        @Schema(example = "DEVELOPER")
        String name,

        @Size(max = 255)
        @Schema(example = "Application developers with bulk access")
        String description
) {}
