package com.template.api.dtos.auth;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AuthorizationUrlResponse(
        @JsonProperty("authorization_url") String authorizationUrl
) {}
