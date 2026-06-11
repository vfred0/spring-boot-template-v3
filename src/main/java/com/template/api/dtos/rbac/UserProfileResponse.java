package com.template.api.dtos.rbac;

import java.util.List;

public record UserProfileResponse(
        String username,
        String role,
        List<String> permissions
) {}
