package com.template.config.dpop;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.template.config.security.JsonAuthEntryPoint;
import com.template.config.DpopProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Authenticates DPoP-scheme requests itself, ahead of the standard
 * {@code BearerTokenAuthenticationFilter}. That filter unconditionally rejects any
 * already-authenticated result whose attributes contain {@code cnf.jkt} (Spring
 * Security 6.5.1, {@code isDPoPBoundAccessToken}), regardless of whether a valid DPoP
 * proof was presented — so a DPoP-bound token can never reach this filter's own proof
 * validation if the standard filter runs first. Running first and hiding the
 * Authorization header afterwards keeps the standard filter a harmless no-op for the
 * requests this filter already handled, without switching resource-server modes.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DpopAuthenticationFilter extends OncePerRequestFilter {

    private static final String DPOP_HEADER = "DPoP";
    private static final String DPOP_SCHEME = "DPoP ";
    private static final String CNF = "cnf";
    private static final String JKT = "jkt";
    private static final String ISSUED_AT = "iat";
    private static final String EXPIRES_AT = "exp";

    private final DpopProperties properties;
    private final DpopProofValidator validator;
    private final JsonAuthEntryPoint authEntryPoint;
    private final Optional<OpaqueTokenIntrospector> opaqueTokenIntrospector;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!properties.isEnabled() || !usesDpopScheme(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String dpopProof = request.getHeader(DPOP_HEADER);
        if (!StringUtils.hasText(dpopProof)) {
            reject(request, response, "DPoP proof is required");
            return;
        }

        String accessToken = extractRawToken(request);
        if (!StringUtils.hasText(accessToken)) {
            filterChain.doFilter(request, response);
            return;
        }

        Authentication authentication;
        try {
            authentication = authenticate(accessToken);
        } catch (RuntimeException ex) {
            log.warn("DPoP-bound token authentication failed: {}", ex.getMessage());
            reject(request, response, "Invalid bearer token");
            return;
        }

        Map<String, Object> tokenAttributes = extractTokenAttributes(authentication);
        String expectedJkt = extractExpectedJkt(tokenAttributes);
        String requestUri = buildRequestUri(request);

        try {
            validator.validate(request.getMethod(), requestUri, accessToken, dpopProof, expectedJkt);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(new AuthorizationHeaderHidingRequest(request), response);
        } catch (DpopProofValidationException ex) {
            log.warn("DPoP validation failed: {}", ex.getMessage());
            reject(request, response, ex.getMessage());
        }
    }

    private Authentication authenticate(String accessToken) {
        if (opaqueTokenIntrospector.isEmpty()) {
            throw new IllegalStateException("No OpaqueTokenIntrospector available to authenticate a DPoP-bound token");
        }

        OAuth2AuthenticatedPrincipal principal = opaqueTokenIntrospector.get().introspect(accessToken);
        OAuth2AccessToken token = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                accessToken,
                instantAttribute(principal, ISSUED_AT),
                instantAttribute(principal, EXPIRES_AT)
        );
        return new BearerTokenAuthentication(principal, token, principal.getAuthorities());
    }

    private Instant instantAttribute(OAuth2AuthenticatedPrincipal principal, String name) {
        return principal.getAttribute(name) instanceof Instant instant ? instant : null;
    }

    private void reject(HttpServletRequest request, HttpServletResponse response, String message) throws IOException {
        authEntryPoint.commence(request, response, new InsufficientAuthenticationException(message));
    }

    private boolean usesDpopScheme(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        return StringUtils.hasText(authorization)
                && authorization.regionMatches(true, 0, DPOP_SCHEME, 0, DPOP_SCHEME.length());
    }

    private String extractRawToken(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        String token = authorization.substring(DPOP_SCHEME.length()).trim();
        return token.isEmpty() ? null : token;
    }

    private String buildRequestUri(HttpServletRequest request) {
        StringBuilder requestUri = new StringBuilder(request.getRequestURL());
        if (StringUtils.hasText(request.getQueryString())) {
            requestUri.append('?').append(request.getQueryString());
        }
        return requestUri.toString();
    }

    private Map<String, Object> extractTokenAttributes(Authentication authentication) {
        if (authentication instanceof BearerTokenAuthentication bearer) {
            return bearer.getTokenAttributes();
        }
        return Map.of();
    }

    @SuppressWarnings("unchecked")
    private String extractExpectedJkt(Map<String, Object> attributes) {
        if (attributes == null || attributes.isEmpty()) {
            return null;
        }

        Object cnf = attributes.get(CNF);
        if (cnf instanceof Map<?, ?> cnfMap) {
            Object jkt = cnfMap.get(JKT);
            if (jkt instanceof String thumbprint && StringUtils.hasText(thumbprint)) {
                return thumbprint;
            }
        }

        return null;
    }

    /**
     * Hides the Authorization header from filters running after this one, so the
     * standard {@code BearerTokenAuthenticationFilter} resolves no token for a request
     * this filter already authenticated, and leaves the SecurityContext untouched.
     */
    private static final class AuthorizationHeaderHidingRequest extends HttpServletRequestWrapper {

        AuthorizationHeaderHidingRequest(HttpServletRequest request) {
            super(request);
        }

        @Override
        public String getHeader(String name) {
            return HttpHeaders.AUTHORIZATION.equalsIgnoreCase(name) ? null : super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            return HttpHeaders.AUTHORIZATION.equalsIgnoreCase(name)
                    ? Collections.emptyEnumeration()
                    : super.getHeaders(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            List<String> names = Collections.list(super.getHeaderNames()).stream()
                    .filter(name -> !HttpHeaders.AUTHORIZATION.equalsIgnoreCase(name))
                    .toList();
            return Collections.enumeration(names);
        }
    }
}
