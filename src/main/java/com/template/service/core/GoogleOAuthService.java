package com.template.service.core;

import com.template.api.dtos.auth.KeycloakTokenResponse;
import com.template.api.http_errors.exceptions.KeycloakAuthException;
import com.template.config.keycloak.KeycloakProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleOAuthService {

    private static final String EXCHANGE_FAILED = "OAuth2 code exchange failed";

    private final KeycloakProperties props;
    private final RestTemplate rest;

    public String buildAuthorizationUrl(String state) {
        return UriComponentsBuilder.fromUriString(props.getPublicAuthorizationUrl())
                .queryParam("response_type", "code")
                .queryParam("client_id", props.getResourceClientId())
                .queryParam("scope", "openid profile email")
                .queryParam("redirect_uri", props.getOauth2RedirectUri())
                .queryParam("state", state)
                .queryParam("kc_idp_hint", "google")
                .queryParam("prompt", "select_account")
                .toUriString();
    }

    public KeycloakTokenResponse exchangeCode(String code) {
        HttpEntity<MultiValueMap<String, String>> entity = buildExchangeEntity(code);

        log.info("Exchanging OAuth2 authorization code with Keycloak");

        try {
            ResponseEntity<KeycloakTokenResponse> response =
                    rest.postForEntity(props.getTokenUrl(), entity, KeycloakTokenResponse.class);

            if (response.getBody() == null) {
                throw new KeycloakAuthException(EXCHANGE_FAILED, HttpStatus.BAD_REQUEST, "empty_response");
            }

            return response.getBody();

        } catch (HttpStatusCodeException ex) {
            log.error("OAuth2 code exchange error: status={}, body={}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new KeycloakAuthException(EXCHANGE_FAILED, HttpStatus.valueOf(ex.getStatusCode().value()), "invalid_grant");
        }
    }

    private HttpEntity<MultiValueMap<String, String>> buildExchangeEntity(String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("code", code);
        form.add("redirect_uri", props.getOauth2RedirectUri());
        form.add("client_id", props.getResourceClientId());
        form.add("client_secret", props.getResourceClientSecret());

        return new HttpEntity<>(form, headers);
    }
}
