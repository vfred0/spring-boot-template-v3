package com.template.security;

import com.template.config.dpop.DpopAwareBearerTokenResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class DpopAwareBearerTokenResolverTest {

    private final DpopAwareBearerTokenResolver resolver = new DpopAwareBearerTokenResolver();

    @ParameterizedTest
    @MethodSource("supportedAuthorizationHeaders")
    void resolvesSupportedAuthorizationScheme(String authorization, String expectedToken) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, authorization);

        String token = resolver.resolve(request);

        assertThat(token).isEqualTo(expectedToken);
    }

    @Test
    void returnsNullForMissingAuthorization() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        String token = resolver.resolve(request);

        assertThat(token).isNull();
    }

    @Test
    void returnsNullWhenDpopAuthorizationTokenIsBlank() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "DPoP   ");

        String token = resolver.resolve(request);

        assertThat(token).isNull();
    }

    private static Stream<Arguments> supportedAuthorizationHeaders() {
        return Stream.of(
                Arguments.of("DPoP token-value", "token-value"),
                Arguments.of("Bearer token-value", "token-value"),
                Arguments.of("dPoP token-value", "token-value")
        );
    }
}
