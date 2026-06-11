package com.template.api.dtos.rbac;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PermissionRef(

        @NotBlank
        @Size(max = 100)
        @Schema(example = "customers")
        String resource,

        @NotBlank
        @Size(max = 100)
        @Schema(example = "read")
        String action
) {}
