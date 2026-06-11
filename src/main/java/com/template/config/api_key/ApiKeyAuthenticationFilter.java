package com.template.config.api_key;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import com.template.config.security.JsonAuthEntryPoint;
import com.template.config.security.SecurityMode;
import com.template.config.security.SecurityProperties;
import com.template.service.core.api_key.ApiKeyAuthenticationService;
import com.template.service.core.rbac.AuthAuditEvent;
import com.template.service.core.rbac.AuthAuditService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private static final String USER_AGENT_HEADER = "User-Agent";

    private final ApiKeyProperties properties;
    private final SecurityProperties securityProperties;
    private final ApiKeyAuthenticationService authenticationService;
    private final AuthAuditService auditService;
    private final JsonAuthEntryPoint jsonAuthEntryPoint;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (securityProperties.getMode() != SecurityMode.API_KEY) {
            filterChain.doFilter(request, response);
            return;
        }

        String rawKey = request.getHeader(properties.getHeader());
        if (!StringUtils.hasText(rawKey)) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = request.getRemoteAddr();
        ApiKeyAuthResult result = authenticationService.authenticate(rawKey, clientIp);
        auditService.record(toEvent(request, clientIp, result));

        if (result.isSuccess()) {
            SecurityContextHolder.getContext().setAuthentication(result.authentication());
            filterChain.doFilter(request, response);
            return;
        }

        SecurityContextHolder.clearContext();
        jsonAuthEntryPoint.commence(request, response, new BadCredentialsException("Invalid API key"));
    }

    private AuthAuditEvent toEvent(HttpServletRequest request, String clientIp, ApiKeyAuthResult result) {
        return new AuthAuditEvent(
                result.subject(),
                result.apiKeyId(),
                clientIp,
                request.getHeader(USER_AGENT_HEADER),
                result.outcome(),
                request.getServletPath(),
                request.getMethod()
        );
    }
}
