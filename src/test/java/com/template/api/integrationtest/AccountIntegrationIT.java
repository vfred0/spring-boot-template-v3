package com.template.api.integrationtest;

import com.template.MainApplication;
import com.template.api.util.KeycloakIntegrationTest;
import com.template.api.dtos.account.AccountResponse;
import com.template.api.dtos.core.ApiResult;
import com.template.api.dtos.account.UpdateBalanceRequest;
import com.template.data.entities.core.rbac.Account;
import com.template.data.entities.core.Client;
import com.template.data.daos.AccountRepository;
import com.template.data.daos.ClientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.template.config.keycloak.KeycloakProperties;
import com.template.config.security.RateLimitingFilter;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = MainApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class AccountIntegrationIT extends KeycloakIntegrationTest {

    private final AccountRepository accountRepository;
    private final ClientRepository clientRepository;

    AccountIntegrationIT(@Qualifier("keycloakProperties") KeycloakProperties props,
                         CacheManager cacheManager,
                         RateLimitingFilter rateLimitingFilter,
                         AccountRepository accountRepository,
                         ClientRepository clientRepository) {
        super(props, cacheManager, rateLimitingFilter);
        this.accountRepository = accountRepository;
        this.clientRepository = clientRepository;
    }

    @BeforeEach
    void setUp() {
        accountRepository.deleteAll();
        clientRepository.deleteAll();
    }

    @Test
    void update_balance_pessimistic_success() {
        Account account = saveAccount("100.00", "+37061111111");
        String token = loginAndGetAccess(USERNAME, USER_PASSWORD);

        AccountResponse response = postAndReturnData(
                accountUrl + "/balance/pessimistic",
                token,
                new UpdateBalanceRequest(account.getClient().getId(), new BigDecimal("250.75")),
                AccountResponse.class
        );

        assertThat(response.accountId()).isEqualTo(account.getId());
        assertThat(response.clientId()).isEqualTo(account.getClient().getId());
        assertThat(response.balance()).isEqualByComparingTo("350.75");

        Account persisted = accountRepository.findById(account.getId()).orElseThrow();
        assertThat(persisted.getBalance()).isEqualByComparingTo("350.75");
    }

    @Test
    void update_balance_optimistic_success() {
        Account account = saveAccount("50.00", "+37062222222");
        String token = loginAndGetAccess(USERNAME, USER_PASSWORD);

        AccountResponse response = postAndReturnData(
                accountUrl + "/balance/optimistic",
                token,
                new UpdateBalanceRequest(account.getClient().getId(), new BigDecimal("75.00")),
                AccountResponse.class
        );

        assertThat(response.accountId()).isEqualTo(account.getId());
        assertThat(response.clientId()).isEqualTo(account.getClient().getId());
        assertThat(response.balance()).isEqualByComparingTo("125.00");
    }

    @Test
    void get_account_by_client_id_success() {
        Account account = saveAccount("10.00", "+37063333333");
        String token = loginAndGetAccess(USERNAME, USER_PASSWORD);

        AccountResponse response = getAndReturnData(
                accountUrl + "/client/" + account.getClient().getId(),
                token,
                AccountResponse.class
        );

        assertThat(response.accountId()).isEqualTo(account.getId());
        assertThat(response.clientId()).isEqualTo(account.getClient().getId());
        assertThat(response.balance()).isEqualByComparingTo("10.00");
    }

    @Test
    void update_balance_pessimistic_not_found_returns_404() {
        String token = loginAndGetAccess(USERNAME, USER_PASSWORD);

        ResponseEntity<ApiResult<Object>> response = requestPost(
                accountUrl + "/balance/pessimistic",
                token,
                null,
                new UpdateBalanceRequest(999999L, new BigDecimal("100.00"))
        );

         assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private Account saveAccount(String balance, String phone) {
        Client client = clientRepository.save(
                Client.builder()
                        .firstName("Test")
                        .lastName("User")
                        .phone(phone)
                        .build()
        );

        return accountRepository.saveAndFlush(
                Account.builder()
                        .balance(new BigDecimal(balance))
                        .client(client)
                        .build()
        );
    }
}
