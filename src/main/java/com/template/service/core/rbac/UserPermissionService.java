package com.template.service.core.rbac;

import lombok.RequiredArgsConstructor;
import com.template.api.dtos.rbac.UserProfileResponse;
import com.template.data.entities.core.rbac.UserRole;
import com.template.data.daos.UserRoleRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserPermissionService {

    private final UserRoleRepository userRoleRepository;

    @Transactional(readOnly = true)
    public List<GrantedAuthority> loadAuthorities(String keycloakSub) {
        return userRoleRepository.findByKeycloakSub(keycloakSub)
                .map(this::toAuthorities)
                .orElse(List.of());
    }

    private List<GrantedAuthority> toAuthorities(UserRole userRole) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + userRole.getRole().getName()));
        userRole.getRole().getPermissions().forEach(p ->
                authorities.add(new SimpleGrantedAuthority(p.getResource() + ":" + p.getAction())));
        return authorities;
    }

    @Transactional(readOnly = true)
    public UserProfileResponse buildProfile(String keycloakSub) {
        return userRoleRepository.findByKeycloakSub(keycloakSub)
                .map(this::toProfile)
                .orElse(new UserProfileResponse(keycloakSub, null, List.of()));
    }

    private UserProfileResponse toProfile(UserRole userRole) {
        List<String> permissions = userRole.getRole().getPermissions().stream()
                .map(p -> p.getResource() + ":" + p.getAction())
                .sorted()
                .collect(Collectors.toList());
        return new UserProfileResponse(userRole.getKeycloakSub(), userRole.getRole().getName(), permissions);
    }
}
