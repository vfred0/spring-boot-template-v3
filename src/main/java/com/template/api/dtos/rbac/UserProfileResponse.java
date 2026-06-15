package com.template.api.dtos.rbac;

import java.util.List;

public record UserProfileResponse(
        String subject,
        String name,
        String role,
        List<String> permissions
) {}
