package com.template.service.core;

import com.template.api.dtos.auth.KeycloakTokenResponse;
import com.template.api.dtos.auth.RegisterRequest;
import com.template.api.http_errors.exceptions.KeycloakAuthException;
import com.template.config.keycloak.KeycloakProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class KeycloakUserService {

    private static final String GRANT_TYPE = "grant_type";
    private static final String CLIENT_CREDENTIALS = "client_credentials";
    private static final String SERVICE_ACCOUNT_TOKEN_FAILED = "service_account_token_failed";
    private static final String USER_EXISTS = "user_exists";
    private static final String USER_CREATION_FAILED = "user_creation_failed";
    private static final String KEYCLOAK_UNAVAILABLE = "keycloak_unavailable";

    private final RestTemplate rest;
    private final KeycloakProperties props;

    public void register(RegisterRequest request) {
        String adminToken = obtainAdminToken();
        createUser(request, adminToken);
        log.info("✅ User registered in Keycloak: username={}", request.username());
    }

    private String obtainAdminToken() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add(GRANT_TYPE, CLIENT_CREDENTIALS);
        form.add(KeycloakAuthService.CLIENT_ID, props.getResourceClientId());
        form.add(KeycloakAuthService.CLIENT_SECRET, props.getResourceClientSecret());
        try {
            KeycloakTokenResponse token = rest.postForObject(
                    props.getTokenUrl(), new HttpEntity<>(form, formHeaders()), KeycloakTokenResponse.class);
            return extractAccessToken(token);
        } catch (HttpStatusCodeException ex) {
            log.error("❌ Service account token error: status={}, body={}",
                    ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new KeycloakAuthException(
                    "Service account token request failed",
                    HttpStatus.SERVICE_UNAVAILABLE, SERVICE_ACCOUNT_TOKEN_FAILED);
        } catch (ResourceAccessException ex) {
            throw new KeycloakAuthException(
                    "Keycloak unreachable", HttpStatus.SERVICE_UNAVAILABLE, KEYCLOAK_UNAVAILABLE);
        }
    }

    private String extractAccessToken(KeycloakTokenResponse token) {
        if (token == null || !StringUtils.hasText(token.getAccessToken())) {
            throw new KeycloakAuthException(
                    "Empty service account token response",
                    HttpStatus.SERVICE_UNAVAILABLE, SERVICE_ACCOUNT_TOKEN_FAILED);
        }
        return token.getAccessToken();
    }

    private void createUser(RegisterRequest request, String adminToken) {
        HttpEntity<Map<String, Object>> entity =
                new HttpEntity<>(buildUserPayload(request), jsonHeaders(adminToken));
        try {
            rest.postForEntity(props.getUsersUrl(), entity, Void.class);
        } catch (HttpStatusCodeException ex) {
            throw translateCreateError(ex);
        } catch (ResourceAccessException ex) {
            throw new KeycloakAuthException(
                    "Keycloak unreachable", HttpStatus.SERVICE_UNAVAILABLE, KEYCLOAK_UNAVAILABLE);
        }
    }

    private Map<String, Object> buildUserPayload(RegisterRequest request) {
        return Map.of(
                "username", request.username(),
                "email", request.email(),
                "firstName", request.firstName(),
                "lastName", request.lastName(),
                "enabled", true,
                "credentials", List.of(Map.of(
                        "type", "password",
                        "value", request.password(),
                        "temporary", false)));
    }

    private KeycloakAuthException translateCreateError(HttpStatusCodeException ex) {
        if (ex.getStatusCode().value() == HttpStatus.CONFLICT.value()) {
            return new KeycloakAuthException(
                    "User already exists", HttpStatus.CONFLICT, USER_EXISTS);
        }
        log.error("❌ User creation error: status={}, body={}",
                ex.getStatusCode(), ex.getResponseBodyAsString());
        return new KeycloakAuthException(
                "User creation failed", HttpStatus.SERVICE_UNAVAILABLE, USER_CREATION_FAILED);
    }

    private HttpHeaders formHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        return headers;
    }

    private HttpHeaders jsonHeaders(String adminToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(adminToken);
        return headers;
    }
}
