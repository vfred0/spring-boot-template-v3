package com.template.api.dtos.auth;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TokenResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("token_type") String tokenType,
        @JsonProperty("expires_in") long expiresIn
) {
    public static TokenResponse from(KeycloakTokenResponse keycloak) {
        return new TokenResponse(
                keycloak.getAccessToken(),
                keycloak.getTokenType(),
                keycloak.getExpiresIn()
        );
    }
}
