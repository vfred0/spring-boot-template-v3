package com.template.controller;

import com.template.api.resources.AccountResource;
import com.template.api.dtos.account.AccountResponse;
import com.template.api.dtos.account.UpdateBalanceRequest;
import com.template.service.core.AccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountControllerTest {

    @Mock
    private AccountService accountService;

    private AccountResource accountResource;

    @BeforeEach
    void setUp() {
        accountResource = new AccountResource(accountService);
    }

    @Test
    void updateBalancePessimisticReturnsServiceResult() {
        UpdateBalanceRequest request = new UpdateBalanceRequest(1L, new BigDecimal("10.00"));
        AccountResponse response = new AccountResponse(2L, 1L, new BigDecimal("110.00"));
        when(accountService.updateBalancePessimistic(request)).thenReturn(response);

        var apiResponse = accountResource.updateBalancePessimistic(request);

        assertThat(apiResponse.code()).isZero();
        assertThat(apiResponse.data()).isEqualTo(response);
        verify(accountService).updateBalancePessimistic(request);
    }

    @Test
    void updateBalanceOptimisticReturnsServiceResult() {
        UpdateBalanceRequest request = new UpdateBalanceRequest(1L, new BigDecimal("10.00"));
        AccountResponse response = new AccountResponse(2L, 1L, new BigDecimal("110.00"));
        when(accountService.updateBalanceOptimistic(request)).thenReturn(response);

        var apiResponse = accountResource.updateBalanceOptimistic(request);

        assertThat(apiResponse.code()).isZero();
        assertThat(apiResponse.data()).isEqualTo(response);
        verify(accountService).updateBalanceOptimistic(request);
    }

    @Test
    void getByClientIdReturnsServiceResult() {
        AccountResponse response = new AccountResponse(2L, 1L, new BigDecimal("110.00"));
        when(accountService.getByClientId(1L)).thenReturn(response);

        var apiResponse = accountResource.getByClientId(1L);

        assertThat(apiResponse.code()).isZero();
        assertThat(apiResponse.data()).isEqualTo(response);
        verify(accountService).getByClientId(1L);
    }
}
