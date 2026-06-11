package com.template.api.dtos.rbac;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record AssignPermissionsRequest(

        @NotEmpty
        List<@Valid PermissionRef> permissions
) {}
