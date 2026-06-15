package com.template.api.integrationtest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.template.MainApplication;
import com.template.api.dtos.core.ApiResult;
import com.template.api.http_errors.ApiErrorType;
import com.template.api.util.KeycloakIntegrationTest;
import com.template.api.dtos.client.ClientResponse;
import com.template.api.dtos.client.CreateClientRequest;
import com.template.api.dtos.auth.RequestAcceptedResponse;
import com.template.api.dtos.auth.RequestStatusResponse;
import com.template.data.entities.core.Client;
import com.template.data.entities.core.rbac.Account;
import com.template.data.entities.core.request.RequestStatus;
import com.template.data.daos.AccountRepository;
import com.template.data.daos.ClientRepository;
import com.template.data.daos.RequestRepository;
import com.template.service.core.ClientService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.http.*;
import com.template.config.keycloak.KeycloakProperties;
import com.template.config.security.RateLimitingFilter;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(
        classes = MainApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class ClientIntegrationIT extends KeycloakIntegrationTest {

    public static final String JOHN = "John";
    public static final String DOE = "Doe";
    public static final String ALICE = "Alice";
    public static final String SMITH = "Smith";
    public static final String PHONE = "+37061234567";
    private final ClientRepository repo;
    private final AccountRepository accountRepository;
    private final RequestRepository requestRepository;

    ClientIntegrationIT(@Qualifier("keycloakProperties") KeycloakProperties props,
                        CacheManager cacheManager,
                        RateLimitingFilter rateLimitingFilter,
                        ClientRepository repo,
                        AccountRepository accountRepository,
                        RequestRepository requestRepository) {
        super(props, cacheManager, rateLimitingFilter);
        this.repo = repo;
        this.accountRepository = accountRepository;
        this.requestRepository = requestRepository;
    }

    @BeforeEach
    void setUp() {
        accountRepository.deleteAll();
        repo.deleteAll();
        requestRepository.deleteAll();
    }

    @Test
    void create_client_success_and_persistence() {
        String token = loginAndGetAccess(USERNAME, USER_PASSWORD);

        CreateClientRequest req = new CreateClientRequest(JOHN, DOE, PHONE);

        RequestAcceptedResponse accepted = postAndReturnData(clientUrl, token, req, HttpStatus.ACCEPTED, RequestAcceptedResponse.class);

        assertThat(accepted.requestId()).isNotNull();
        assertThat(accepted.status()).isEqualTo(RequestStatus.PENDING);

        RequestStatusResponse statusResponse = awaitRequestStatus(token, accepted.requestId(), RequestStatus.COMPLETED);
        ApiResult<ClientResponse> finalResponse = readNestedResponse(statusResponse);
        ClientResponse data = objectMapper.convertValue(finalResponse.data(), ClientResponse.class);

        assertThat(data.phone()).isEqualTo(req.phone());
        assertThat(data.id()).isNotNull();
        assertThat(finalResponse.code()).isZero();

        // Verify persisted
        assertThat(repo.existsByPhone(req.phone())).isTrue();
        assertThat(requestRepository.findById(accepted.requestId())).isPresent();
        Account createdAccount = accountRepository.findByClientId(data.id()).orElse(null);
        assertThat(createdAccount).isNotNull();
        assertThat(createdAccount.getBalance()).isEqualByComparingTo("0");

        // Verify GET by id returns same data
        ClientResponse fetched = getAndReturnData(clientUrl + "/" + data.id(), token, ClientResponse.class);
        assertThat(fetched).isNotNull();
        assertThat(fetched.id()).isEqualTo(data.id());
        assertThat(fetched.phone()).isEqualTo(data.phone());
    }

    @Test
    void create_client_duplicate_phone_conflict() {
        // prepare existing
        Client existing = Client.builder().firstName("Jane").lastName("Roe").phone(PHONE).build();
        repo.save(existing);

        String token = loginAndGetAccess(USERNAME, USER_PASSWORD);
        CreateClientRequest req = new CreateClientRequest(JOHN, DOE, PHONE);

        RequestAcceptedResponse accepted = postAndReturnData(clientUrl, token, req, HttpStatus.ACCEPTED, RequestAcceptedResponse.class);
        RequestStatusResponse statusResponse = awaitRequestStatus(token, accepted.requestId(), RequestStatus.FAILED);
        ApiResult<Object> finalResponse = readNestedResponse(statusResponse);

        assertThat(finalResponse.code()).isEqualTo(ApiErrorType.CONFLICT.code());
        assertThat(finalResponse.message()).isEqualTo("Client with phone=" + req.phone() + " already exists");
    }

    @Test
    void get_client_success() {
        Client saved = repo.save(Client.builder().firstName(ALICE).lastName(SMITH).phone("+37060000000").build());

        String token = loginAndGetAccess(USERNAME, USER_PASSWORD);

        ClientResponse data = getAndReturnData(clientUrl + "/" + saved.getId(), token, ClientResponse.class);

        assertThat(data.id()).isEqualTo(saved.getId());
        assertThat(data.phone()).isEqualTo(saved.getPhone());
    }

    @Test
    void search_clients_success() {
        repo.save(Client.builder().firstName("Alice").lastName("Brown").phone("+37070000001").build());
        repo.save(Client.builder().firstName("Mike").lastName("Alister").phone("+37070000002").build());
        repo.save(Client.builder().firstName("Alix").lastName("Stone").phone("+37070000004").build());
        repo.save(Client.builder().firstName("John").lastName("Doe").phone("+37070000003").build());

        String token = loginAndGetAccess(USERNAME, USER_PASSWORD);

        ResponseEntity<ApiResult<Object>> resp = requestGet(clientUrl + "/search?q=ali", token);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().code()).isZero();

        var clients = objectMapper.convertValue(resp.getBody().data(), new TypeReference<java.util.List<ClientResponse>>() {});
        assertThat(clients).hasSize(2);
        assertThat(clients)
                .extracting(ClientResponse::phone)
                .containsExactly("+37070000001", "+37070000002");
    }

    @Test
    void search_clients_query_too_short_returns_400() {
        String token = loginAndGetAccess(USERNAME, USER_PASSWORD);

        ResponseEntity<ApiResult<Object>> resp = requestGet(clientUrl + "/search?q=ab", token);

        assertErrorStatusAndBody(resp, HttpStatus.BAD_REQUEST,
                ApiErrorType.BAD_REQUEST.code(),
                "Search query must contain at least " + ClientService.MIN_SEARCH_QUERY_LENGTH + " characters");
    }

    @Test
    void search_clients_forbidden_when_user_has_no_role() {
        String token = loginAndGetAccess(ADMIN, ADMIN_PASSWORD);

        ResponseEntity<ApiResult<Object>> resp = requestGet(clientUrl + "/search?q=alice", token);

        assertErrorStatusAndBody(resp, HttpStatus.FORBIDDEN,
                ApiErrorType.FORBIDDEN.code(),
                ApiErrorType.FORBIDDEN.message());
    }

    @Test
    void get_client_not_found_returns_404() {
        String token = loginAndGetAccess(USERNAME, USER_PASSWORD);

        ResponseEntity<ApiResult<Object>> resp = requestGet(clientUrl + "/999999", token);

        assertErrorStatusAndBody(resp, HttpStatus.NOT_FOUND,
                ApiErrorType.NOT_FOUND.code(),
                "Client with id=999999 not found");
    }

    // Additional negative tests for GET
    @Test
    void get_client_unauthorized() {
        Client saved = repo.save(Client.builder().firstName("Bob").lastName("Brown").phone("+37063333333").build());

        ResponseEntity<ApiResult<Object>> resp = requestGet(clientUrl + "/" + saved.getId());

        assertErrorStatusAndBody(resp, HttpStatus.UNAUTHORIZED,
                ApiErrorType.UNAUTHORIZED.code(),
                ApiErrorType.UNAUTHORIZED.message());
    }

    @Test
    void get_client_unauthorized_after_logout() {
        Client saved = repo.save(Client.builder().firstName(ALICE).lastName(SMITH).phone("+37060000000").build());
        String accessToken = loginAndGetAccess(USERNAME, USER_PASSWORD);
        String refreshToken = loginAndGetRefresh(USERNAME, USER_PASSWORD);

        ResponseEntity<ApiResult<Object>> resp = requestGet(clientUrl + "/" + saved.getId(), accessToken);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<ApiResult<Void>> logoutResponse = logoutRequest(refreshToken);
        assertThat(logoutResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<ApiResult<Object>> errorResponse = requestGet(clientUrl + "/" + saved.getId(), accessToken);
        assertErrorStatusAndBody(errorResponse, HttpStatus.UNAUTHORIZED,
                ApiErrorType.UNAUTHORIZED.code(),
                ApiErrorType.UNAUTHORIZED.message());
    }

    @Test
    void get_client_forbidden_when_user_has_no_role() {
        Client saved = repo.save(Client.builder().firstName("Carol").lastName("White").phone("+37064444444").build());

        String token = loginAndGetAccess(ADMIN, ADMIN_PASSWORD);

        ResponseEntity<ApiResult<Object>> resp = requestGet(clientUrl + "/" + saved.getId(), token);

        assertErrorStatusAndBody(resp, HttpStatus.FORBIDDEN,
                ApiErrorType.FORBIDDEN.code(),
                ApiErrorType.FORBIDDEN.message());
    }

    @Test
    void get_client_invalid_id_returns_bad_request() {
        String token = loginAndGetAccess(USERNAME, USER_PASSWORD);

        ResponseEntity<ApiResult<Object>> resp = requestGet(clientUrl + "/invalid-id", token);

        assertErrorStatusAndBody(resp, HttpStatus.BAD_REQUEST,
                ApiErrorType.BAD_REQUEST.code(),
                "Invalid value: invalid-id");
    }

    // negative tests
    @Test
    void create_client_unauthorized() {
        CreateClientRequest req = new CreateClientRequest("No", "Token", "+37061111111");

        ResponseEntity<ApiResult<Object>> resp = requestPost(clientUrl, null, req);

        assertErrorStatusAndBody(resp, HttpStatus.UNAUTHORIZED,
                ApiErrorType.UNAUTHORIZED.code(),
                ApiErrorType.UNAUTHORIZED.message());
    }

    @Test
    void create_client_forbidden_when_user_has_no_role() {
        String token = loginAndGetAccess(ADMIN, ADMIN_PASSWORD);
        CreateClientRequest req = new CreateClientRequest("No", "Role", "+37062222222");

        ResponseEntity<ApiResult<Object>> resp = requestPost(clientUrl, token, null, req);

        assertErrorStatusAndBody(resp, HttpStatus.FORBIDDEN,
                ApiErrorType.FORBIDDEN.code(),
                ApiErrorType.FORBIDDEN.message());
    }

    @Test
    void create_client_validation_error() {
        String token = loginAndGetAccess(USERNAME, USER_PASSWORD);
        // invalid phone and missing firstName
        CreateClientRequest req = new CreateClientRequest("", DOE, "abc");

        ResponseEntity<ApiResult<Object>> response = requestPost(clientUrl, token, null, req);

        Set<String> expected = Set.of(
                "phone: phone must be valid",
                "firstName: firstName is required"
        );

        assertErrorStatusAndBody(response, HttpStatus.BAD_REQUEST,
                ApiErrorType.BAD_REQUEST.code(),
                expected);
    }

    @Test
    void create_client_validation_error_russian_locale() {
        String token = loginAndGetAccess(USERNAME, USER_PASSWORD);
        // invalid phone and missing firstName
        CreateClientRequest req = new CreateClientRequest("", DOE, "abc");

        ResponseEntity<ApiResult<Object>> response = requestPost(clientUrl, token, "ru", req);

        Set<String> expected = Set.of(
                "phone: Неверный формат телефона",
                "firstName: Имя обязательно"
        );

        assertErrorStatusAndBody(response, HttpStatus.BAD_REQUEST,
                ApiErrorType.BAD_REQUEST.code(),
                expected);
    }

    @Test
    void get_request_status_not_found_returns_404() {
        String token = loginAndGetAccess(USERNAME, USER_PASSWORD);

        ResponseEntity<ApiResult<Object>> resp = requestGet(requestUrl + "/" + UUID.randomUUID(), token);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().code()).isEqualTo(ApiErrorType.NOT_FOUND.code());
    }

    @Test
    void get_request_status_unauthorized() {
        ResponseEntity<ApiResult<Object>> resp = requestGet(requestUrl + "/" + UUID.randomUUID());

        assertErrorStatusAndBody(resp, HttpStatus.UNAUTHORIZED,
                ApiErrorType.UNAUTHORIZED.code(),
                ApiErrorType.UNAUTHORIZED.message());
    }

    @Test
    void get_request_status_forbidden_when_user_has_no_role() {
        String token = loginAndGetAccess(ADMIN, ADMIN_PASSWORD);

        ResponseEntity<ApiResult<Object>> resp = requestGet(requestUrl + "/" + UUID.randomUUID(), token);

        assertErrorStatusAndBody(resp, HttpStatus.FORBIDDEN,
                ApiErrorType.FORBIDDEN.code(),
                ApiErrorType.FORBIDDEN.message());
    }

    @Test
    void get_request_status_invalid_id_returns_bad_request() {
        String token = loginAndGetAccess(USERNAME, USER_PASSWORD);

        ResponseEntity<ApiResult<Object>> resp = requestGet(requestUrl + "/invalid-id", token);

        assertErrorStatusAndBody(resp, HttpStatus.BAD_REQUEST,
                ApiErrorType.BAD_REQUEST.code(),
                "Invalid value: invalid-id");
    }

    private RequestStatusResponse awaitRequestStatus(String token, UUID requestId, RequestStatus expectedStatus) {
        final RequestStatusResponse[] holder = new RequestStatusResponse[1];

        await()
                .atMost(15, TimeUnit.SECONDS)
                .pollInterval(250, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    RequestStatusResponse statusResponse = getAndReturnData(requestUrl + "/" + requestId, token, RequestStatusResponse.class);
                    assertThat(statusResponse.status()).isEqualTo(expectedStatus);
                    if (expectedStatus == RequestStatus.COMPLETED || expectedStatus == RequestStatus.FAILED) {
                        assertThat(statusResponse.response()).isNotNull();
                    }
                    holder[0] = statusResponse;
                });

        return holder[0];
    }

    private <T> ApiResult<T> readNestedResponse(RequestStatusResponse statusResponse) {
        return objectMapper.convertValue(statusResponse.response(), new TypeReference<>() {});
    }
}
