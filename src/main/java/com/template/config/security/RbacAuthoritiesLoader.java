package com.template.config.security;

import lombok.RequiredArgsConstructor;
import com.template.service.core.rbac.UserPermissionService;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Component
@RequiredArgsConstructor
public class RbacAuthoritiesLoader {

    private final UserPermissionService userPermissionService;

    public Collection<GrantedAuthority> loadFor(String subject) {
        return userPermissionService.loadAuthorities(subject);
    }
}
