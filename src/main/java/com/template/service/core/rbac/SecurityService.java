package com.template.service.core.rbac;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class SecurityService {

    public static final String ANONYMOUS = "anonymous";

    public String clientId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return ANONYMOUS;

        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            return jwtAuth.getToken().getClaimAsString("azp");
        }

        if (auth instanceof BearerTokenAuthentication bearerAuth) {
            return extractClientId(bearerAuth.getTokenAttributes());
        }

        return ANONYMOUS;
    }

    public String username() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return ANONYMOUS;
        return auth.getName();
    }

    private String extractClientId(Map<String, Object> attributes) {
        if (attributes == null) return ANONYMOUS;
        Object azp = attributes.get("azp");
        if (azp instanceof String azpStr) {
            return azpStr;
        }

        Object clientId = attributes.get("client_id");
        if (clientId instanceof String clientIdStr) {
            return clientIdStr;
        }

        return ANONYMOUS;
    }
}
