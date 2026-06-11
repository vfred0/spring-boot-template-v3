package com.template.service;

import com.template.config.keycloak.KeycloakProperties;
import com.template.api.dtos.auth.KeycloakTokenResponse;
import com.template.api.dtos.auth.SignOutRequest;
import com.template.api.dtos.auth.RefreshRequest;
import com.template.api.http_errors.exceptions.KeycloakAuthException;
import com.template.service.core.KeycloakAuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KeycloakAuthServiceTest {

    private static final String CLIENT_ID = "client";
    private static final String CLIENT_SECRET = "secret";
    private static final String REFRESH_TOKEN = "refresh";

    @Mock
    private RestTemplate rest;

    @Mock
    private KeycloakProperties props;

    @Test
    void refreshThrowsWhenResponseBodyMissing() {
        when(props.getTokenUrl()).thenReturn("http://token");
        when(rest.postForEntity(
                anyString(),
                any(HttpEntity.class),
                eq(KeycloakTokenResponse.class)
        )).thenReturn(ResponseEntity.ok().body(null));

        KeycloakAuthService service = new KeycloakAuthService(rest, props);
        RefreshRequest request = new RefreshRequest(CLIENT_ID, CLIENT_SECRET);

        assertThatThrownBy(() -> service.refresh(request, REFRESH_TOKEN))
                .isInstanceOf(KeycloakAuthException.class)
                .hasMessage("Empty response")
                .satisfies(ex -> {
                    KeycloakAuthException authEx = (KeycloakAuthException) ex;
                    assertThat(authEx.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(authEx.getKeycloakMessage()).isEqualTo("invalid_grant");
                });
    }

    @Test
    void logoutThrowsWhenInvalidTokenReturned() {
        when(props.getLogoutUrl()).thenReturn("http://logout");
        when(rest.postForEntity(
                anyString(),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(ResponseEntity.ok("invalid_token"));

        KeycloakAuthService service = new KeycloakAuthService(rest, props);
        SignOutRequest request = new SignOutRequest(CLIENT_ID, CLIENT_SECRET);

        assertThatThrownBy(() -> service.logout(request, REFRESH_TOKEN))
                .isInstanceOf(KeycloakAuthException.class)
                .hasMessage("invalid_token")
                .satisfies(ex -> {
                    KeycloakAuthException authEx = (KeycloakAuthException) ex;
                    assertThat(authEx.getStatus()).isEqualTo(HttpStatus.OK);
                    assertThat(authEx.getKeycloakMessage()).isEqualTo("invalid_token");
                });
    }

    @Test
    void extractErrorMessageHandlesNullBody() {
        KeycloakAuthService service = new KeycloakAuthService(rest, props);

        String result = ReflectionTestUtils.invokeMethod(service, "extractErrorMessage", (String) null);

        assertThat(result).isEqualTo("invalid_grant");
    }

    @Test
    void extractErrorMessageHandlesNotAllowed() {
        KeycloakAuthService service = new KeycloakAuthService(rest, props);

        String result = ReflectionTestUtils.invokeMethod(service, "extractErrorMessage", "not_allowed");

        assertThat(result).isEqualTo("not_allowed");
    }
}
