package com.template.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.template.config.security.JsonAccessDeniedHandler;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThat;

class JsonAccessDeniedHandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JsonAccessDeniedHandler handler = new JsonAccessDeniedHandler();

    @Test
    void handleSetsStatusAndJsonBody() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.handle(request, response, new AccessDeniedException("nope"));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_JSON.toString());

        JsonNode body = objectMapper.readTree(response.getContentAsByteArray());
        assertThat(body.get("code").asInt()).isEqualTo(40301);
        assertThat(body.get("message").asText()).isEqualTo("Forbidden");
        assertThat(body.hasNonNull("data")).isFalse();
    }
}

