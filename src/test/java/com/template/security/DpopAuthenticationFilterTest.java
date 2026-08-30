package com.template.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import com.template.config.security.JsonAuthEntryPoint;
import com.template.config.DpopProperties;
import com.template.config.dpop.DpopAuthenticationFilter;
import com.template.config.dpop.DpopProofValidationException;
import com.template.config.dpop.DpopProofValidator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.DefaultOAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DpopAuthenticationFilterTest {

    @Mock
    private DpopProofValidator validator;

    @Mock
    private JsonAuthEntryPoint authEntryPoint;

    @Mock
    private FilterChain filterChain;

    @Mock
    private OpaqueTokenIntrospector introspector;

    private DpopProperties properties;
    private DpopAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        properties = new DpopProperties();
        properties.setEnabled(true);
        filter = new DpopAuthenticationFilter(properties, validator, authEntryPoint, Optional.of(introspector));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void skipsWhenDpopIsDisabled() throws Exception {
        properties.setEnabled(false);
        MockHttpServletRequest request = request("DPoP token", "proof");

        filter.doFilter(request, new MockHttpServletResponse(), filterChain);

        verify(filterChain).doFilter(any(), any());
        verifyNoInteractions(validator, authEntryPoint, introspector);
    }

    @Test
    void skipsPlainBearerRequests() throws Exception {
        MockHttpServletRequest request = request("Bearer token", null);

        filter.doFilter(request, new MockHttpServletResponse(), filterChain);

        verify(filterChain).doFilter(any(), any());
        verifyNoInteractions(validator, authEntryPoint, introspector);
    }

    @Test
    void rejectsWhenProofHeaderMissing() throws Exception {
        MockHttpServletRequest request = request("DPoP token", null);

        filter.doFilter(request, new MockHttpServletResponse(), filterChain);

        verify(authEntryPoint).commence(any(), any(), argThat(ex ->
                ex instanceof InsufficientAuthenticationException
                        && ex.getMessage().contains("proof is required")));
        verify(filterChain, never()).doFilter(any(), any());
        verifyNoInteractions(validator, introspector);
    }

    @Test
    void rejectsWhenIntrospectionFails() throws Exception {
        MockHttpServletRequest request = request("DPoP token", "proof");
        when(introspector.introspect("token")).thenThrow(new IllegalStateException("inactive token"));

        filter.doFilter(request, new MockHttpServletResponse(), filterChain);

        verify(authEntryPoint).commence(any(), any(), argThat(ex ->
                ex instanceof InsufficientAuthenticationException
                        && ex.getMessage().contains("Invalid bearer token")));
        verify(filterChain, never()).doFilter(any(), any());
        verifyNoInteractions(validator);
    }

    @Test
    void rejectsWhenNoIntrospectorIsAvailable() throws Exception {
        filter = new DpopAuthenticationFilter(properties, validator, authEntryPoint, Optional.empty());
        MockHttpServletRequest request = request("DPoP token", "proof");

        filter.doFilter(request, new MockHttpServletResponse(), filterChain);

        verify(authEntryPoint).commence(any(), any(), argThat(ex ->
                ex instanceof InsufficientAuthenticationException
                        && ex.getMessage().contains("Invalid bearer token")));
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void rejectsWhenProofValidationFails() throws Exception {
        MockHttpServletRequest request = request("DPoP token", "proof");
        when(introspector.introspect("token")).thenReturn(principal(Map.of("cnf", Map.of("jkt", "thumbprint"))));
        doThrow(new DpopProofValidationException("bad proof"))
                .when(validator).validate(any(), any(), any(), any(), any());

        filter.doFilter(request, new MockHttpServletResponse(), filterChain);

        verify(authEntryPoint).commence(any(), any(), argThat(ex ->
                ex instanceof InsufficientAuthenticationException
                        && ex.getMessage().contains("bad proof")));
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void authenticatesAndHidesAuthorizationHeaderWhenProofIsValid() throws Exception {
        MockHttpServletRequest request = request("DPoP token", "proof");
        when(introspector.introspect("token")).thenReturn(principal(Map.of("cnf", Map.of("jkt", "thumbprint"))));

        filter.doFilter(request, new MockHttpServletResponse(), filterChain);

        verify(validator).validate(
                eq("GET"), eq("http://localhost:8080/api/clients/1"), eq("token"), eq("proof"), eq("thumbprint"));
        assertThat(SecurityContextHolder.getContext().getAuthentication())
                .isInstanceOf(BearerTokenAuthentication.class);
        verifyNoInteractions(authEntryPoint);

        ArgumentCaptor<ServletRequest> forwarded = ArgumentCaptor.forClass(ServletRequest.class);
        verify(filterChain).doFilter(forwarded.capture(), any());
        var wrappedRequest = (HttpServletRequest) forwarded.getValue();
        assertThat(wrappedRequest.getHeader(HttpHeaders.AUTHORIZATION)).isNull();
    }

    @Test
    void authenticatesWithoutJktWhenTokenIsNotDpopBound() throws Exception {
        MockHttpServletRequest request = request("DPoP token", "proof");
        when(introspector.introspect("token")).thenReturn(principal(Map.of()));

        filter.doFilter(request, new MockHttpServletResponse(), filterChain);

        verify(validator).validate(eq("GET"), eq("http://localhost:8080/api/clients/1"), eq("token"), eq("proof"), eq(null));
        verify(filterChain).doFilter(any(), any());
    }

    private MockHttpServletRequest request(String authorization, String dpopProof) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/clients/1");
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(8080);
        if (authorization != null) {
            request.addHeader(HttpHeaders.AUTHORIZATION, authorization);
        }
        if (dpopProof != null) {
            request.addHeader("DPoP", dpopProof);
        }
        return request;
    }

    private OAuth2AuthenticatedPrincipal principal(Map<String, Object> extraAttributes) {
        Map<String, Object> attributes = new java.util.HashMap<>(Map.of("sub", "user"));
        attributes.putAll(extraAttributes);
        return new DefaultOAuth2AuthenticatedPrincipal(
                "user", attributes, List.of(new SimpleGrantedAuthority("ROLE_CLIENT_GET")));
    }
}
