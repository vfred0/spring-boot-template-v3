package com.template.api.dtos.rbac;

import com.template.data.entities.core.rbac.Role;

import java.util.List;

public record RoleResponse(
        Long id,
        String name,
        String description,
        List<String> permissions
) {
    public static RoleResponse from(Role role) {
        List<String> permissions = role.getPermissions().stream()
                .map(permission -> permission.getResource() + ":" + permission.getAction())
                .sorted()
                .toList();
        return new RoleResponse(role.getId(), role.getName(), role.getDescription(), permissions);
    }
}
