package com.template.config.keycloak;

import lombok.RequiredArgsConstructor;
import com.template.config.security.RbacAuthoritiesLoader;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;

@Component
@RequiredArgsConstructor
public class KeycloakJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final KeycloakOpaqueRoleConverter roleConverter;
    private final RbacAuthoritiesLoader rbacAuthoritiesLoader;

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = new ArrayList<>();
        authorities.addAll(roleConverter.convert(jwt.getClaims()));
        authorities.addAll(rbacAuthoritiesLoader.loadFor(jwt.getSubject()));

        return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
    }
}
