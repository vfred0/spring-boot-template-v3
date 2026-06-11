package com.template.service.core.api_key;

import lombok.RequiredArgsConstructor;
import com.template.config.api_key.ApiKeyProperties;
import com.template.data.entities.core.ApiKey;
import com.template.data.entities.core.audit.AuthAuditOutcome;
import com.template.data.daos.ApiKeyRepository;
import com.template.config.api_key.ApiKeyAuthResult;
import com.template.config.api_key.ApiKeyHasher;
import com.template.config.security.RbacAuthoritiesLoader;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.security.web.util.matcher.IpAddressMatcher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ApiKeyAuthenticationService {

    private final ApiKeyHasher hasher;
    private final ApiKeyRepository apiKeyRepository;
    private final RbacAuthoritiesLoader rbacAuthoritiesLoader;
    private final ApiKeyProperties properties;

    @Transactional
    public ApiKeyAuthResult authenticate(String rawKey, String clientIp) {
        Optional<ApiKey> found = apiKeyRepository.findByKeyHash(hasher.hash(rawKey));
        if (found.isEmpty()) {
            return ApiKeyAuthResult.failure(AuthAuditOutcome.NOT_FOUND, null, null);
        }

        ApiKey apiKey = found.get();
        AuthAuditOutcome rejection = evaluate(apiKey, clientIp);
        if (rejection != null) {
            return ApiKeyAuthResult.failure(rejection, apiKey.getId(), apiKey.getSubject());
        }

        touch(apiKey, clientIp);
        return ApiKeyAuthResult.success(buildAuthentication(apiKey), apiKey.getId(), apiKey.getSubject());
    }

    private AuthAuditOutcome evaluate(ApiKey apiKey, String clientIp) {
        Instant now = Instant.now();
        if (apiKey.getRevokedAt() != null) {
            return AuthAuditOutcome.REVOKED;
        }
        if (apiKey.getExpiresAt() != null && !apiKey.getExpiresAt().isAfter(now)) {
            return AuthAuditOutcome.EXPIRED;
        }
        if (!ipAllowed(apiKey.getAllowedIps(), clientIp)) {
            return AuthAuditOutcome.IP_BLOCKED;
        }
        return null;
    }

    private boolean ipAllowed(String allowedIps, String clientIp) {
        if (!StringUtils.hasText(allowedIps)) {
            return true;
        }
        if (!StringUtils.hasText(clientIp)) {
            return false;
        }
        return Arrays.stream(allowedIps.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .anyMatch(cidr -> new IpAddressMatcher(cidr).matches(clientIp));
    }

    private void touch(ApiKey apiKey, String clientIp) {
        Instant now = Instant.now();
        Instant last = apiKey.getLastUsedAt();
        if (last == null || last.plus(properties.getLastUsedThrottle()).isBefore(now)) {
            apiKey.setLastUsedAt(now);
            apiKey.setLastUsedIp(clientIp);
        }
    }

    private Authentication buildAuthentication(ApiKey apiKey) {
        Collection<GrantedAuthority> authorities = new ArrayList<>(rbacAuthoritiesLoader.loadFor(apiKey.getSubject()));
        AbstractAuthenticationToken authentication =
                new PreAuthenticatedAuthenticationToken(apiKey.getSubject(), null, authorities);
        return authentication;
    }
}
