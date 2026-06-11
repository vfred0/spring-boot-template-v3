package com.template.api.integrationtest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.template.MainApplication;
import com.template.api.util.AbstractIntegrationTest;
import com.template.config.keycloak.KeycloakProperties;
import com.template.api.dtos.rbac.AccessProbeResponse;
import com.template.api.dtos.core.ApiResult;
import com.template.data.entities.core.rbac.Role;
import com.template.data.entities.core.rbac.UserRole;
import com.template.data.daos.ApiKeyRepository;
import com.template.data.daos.RoleRepository;
import com.template.data.daos.UserRoleRepository;
import com.template.config.security.RateLimitingFilter;
import com.template.service.core.api_key.ApiKeyManagementService;
import com.template.service.core.api_key.CreateApiKeyCommand;
import com.template.service.core.api_key.IssuedApiKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.http.*;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = MainApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@TestPropertySource(properties = "app.security.mode=API_KEY")
class ApiKeyRbacIT extends AbstractIntegrationTest {

    private static final String API_KEY_HEADER = "X-API-Key";
    private static final String ADMIN_SUBJECT = "demo-admin";
    private static final String ANALYST_SUBJECT = "demo-analyst";

    private static final List<String> ALL_ENDPOINTS = List.of(
            "/admin", "/customers/read", "/customers/update",
            "/customers/delete", "/reports/read", "/reports/create");

    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final ApiKeyManagementService apiKeyManagementService;

    private String adminKey;
    private String analystKey;
    private String demoUrl;

    ApiKeyRbacIT(@Qualifier("keycloakProperties") KeycloakProperties props,
                 CacheManager cacheManager,
                 RateLimitingFilter rateLimitingFilter,
                 RoleRepository roleRepository,
                 UserRoleRepository userRoleRepository,
                 ApiKeyRepository apiKeyRepository,
                 ApiKeyManagementService apiKeyManagementService) {
        super(props, cacheManager, rateLimitingFilter);
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.apiKeyRepository = apiKeyRepository;
        this.apiKeyManagementService = apiKeyManagementService;
    }

    @BeforeEach
    void seedRbac() {
        apiKeyRepository.deleteAll();
        userRoleRepository.deleteAll();

        Role admin = roleRepository.findByName("ADMIN").orElseThrow();
        Role analyst = roleRepository.findByName("DATA_ANALYST").orElseThrow();
        linkSubjectToRole(ADMIN_SUBJECT, admin);
        linkSubjectToRole(ANALYST_SUBJECT, analyst);

        adminKey = issueKeyFor(ADMIN_SUBJECT);
        analystKey = issueKeyFor(ANALYST_SUBJECT);

        demoUrl = mainUrl + "/demo";
    }

    @Test
    void admin_key_accesses_every_rbac_endpoint() {
        for (String endpoint : ALL_ENDPOINTS) {
            assertThat(statusOf(endpoint, adminKey)).as(endpoint).isEqualTo(HttpStatus.OK);
        }
    }

    @Test
    void analyst_key_accesses_only_its_permissions() {
        assertThat(statusOf("/customers/read", analystKey)).isEqualTo(HttpStatus.OK);
        assertThat(statusOf("/customers/update", analystKey)).isEqualTo(HttpStatus.OK);

        for (String forbidden : List.of("/customers/delete", "/reports/read", "/reports/create", "/admin")) {
            assertThat(statusOf(forbidden, analystKey)).as(forbidden).isEqualTo(HttpStatus.FORBIDDEN);
        }
    }

    @Test
    void admin_probe_body_exposes_subject_and_all_authorities() throws Exception {
        AccessProbeResponse body = probeBody("/admin", adminKey);

        assertThat(body.subject()).isEqualTo(ADMIN_SUBJECT);
        assertThat(body.granted()).isEqualTo("ROLE_ADMIN");
        assertThat(body.authorities()).contains(
                "ROLE_ADMIN", "customers:read", "customers:update",
                "customers:delete", "reports:read", "reports:create");
    }

    @Test
    void analyst_probe_body_lists_only_granted_authorities() throws Exception {
        AccessProbeResponse body = probeBody("/customers/read", analystKey);

        assertThat(body.subject()).isEqualTo(ANALYST_SUBJECT);
        assertThat(body.authorities())
                .containsExactlyInAnyOrder("ROLE_DATA_ANALYST", "customers:read", "customers:update");
    }

    @Test
    void missing_api_key_is_unauthorized() {
        assertThat(statusOf("/admin", null)).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void invalid_api_key_is_unauthorized() {
        assertThat(statusOf("/admin", "sk_live_does_not_exist")).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void revoked_api_key_is_unauthorized() {
        IssuedApiKey issued = apiKeyManagementService.issue(
                new CreateApiKeyCommand(ADMIN_SUBJECT, "temp key", null, null));
        apiKeyManagementService.revoke(issued.id());

        assertThat(statusOf("/admin", issued.rawKey())).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private void linkSubjectToRole(String subject, Role role) {
        UserRole userRole = new UserRole();
        userRole.setKeycloakSub(subject);
        userRole.setRole(role);
        userRoleRepository.save(userRole);
    }

    private String issueKeyFor(String subject) {
        return apiKeyManagementService.issue(new CreateApiKeyCommand(subject, subject + " key", null, null)).rawKey();
    }

    private HttpStatusCode statusOf(String endpoint, String apiKey) {
        return exchange(endpoint, apiKey).getStatusCode();
    }

    private AccessProbeResponse probeBody(String endpoint, String apiKey) throws Exception {
        ObjectMapper mapper = objectMapper;
        ApiResult<?> response = mapper.readValue(exchange(endpoint, apiKey).getBody(), ApiResult.class);
        return mapper.convertValue(response.data(), AccessProbeResponse.class);
    }

    private ResponseEntity<String> exchange(String endpoint, String apiKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON, MediaType.ALL));
        if (apiKey != null) {
            headers.set(API_KEY_HEADER, apiKey);
        }
        return restTemplate.exchange(demoUrl + endpoint, HttpMethod.GET, new HttpEntity<>(headers), String.class);
    }
}
