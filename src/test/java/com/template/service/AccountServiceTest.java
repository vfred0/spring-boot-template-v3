package com.template.service;

import com.template.data.entities.core.Client;
import jakarta.persistence.OptimisticLockException;
import com.template.api.dtos.account.AccountResponse;
import com.template.api.dtos.account.UpdateBalanceRequest;
import com.template.api.http_errors.exceptions.AccountNotFoundException;
import com.template.api.http_errors.exceptions.AccountOptimisticLockException;
import com.template.config.mapper.AccountMapper;
import com.template.data.entities.core.rbac.Account;
import com.template.data.daos.AccountRepository;
import com.template.service.core.AccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.math.BigDecimal;
import java.util.Optional;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountMapper accountMapper;

    @Mock
    private PlatformTransactionManager transactionManager;

    private AccountService accountService;

    @BeforeEach
    void setUp() {
        lenient().when(transactionManager.getTransaction(any(TransactionDefinition.class)))
                .thenReturn(new SimpleTransactionStatus());
        accountService = new AccountService(accountRepository, accountMapper, transactionManager);
    }

    @Test
    void updateBalancePessimisticUpdatesBalanceAndReturnsMappedResponse() {
        Client client = Client.builder().id(11L).firstName("A").lastName("B").phone("+37060000001").build();
        Account account = Account.builder()
                .id(22L)
                .client(client)
                .balance(new BigDecimal("100.00"))
                .version(0L)
                .build();
        UpdateBalanceRequest request = new UpdateBalanceRequest(11L, new BigDecimal("25.50"));
        AccountResponse response = new AccountResponse(22L, 11L, new BigDecimal("125.50"));

        when(accountRepository.findByClientIdForPessimisticUpdate(11L)).thenReturn(Optional.of(account));
        when(accountRepository.saveAndFlush(account)).thenReturn(account);
        when(accountMapper.toResponse(account)).thenReturn(response);

        AccountResponse actual = accountService.updateBalancePessimistic(request);

        assertThat(actual).isEqualTo(response);
        assertThat(account.getBalance()).isEqualByComparingTo("125.50");
    }

    @Test
    void updateBalancePessimisticThrowsWhenAccountNotFound() {
        when(accountRepository.findByClientIdForPessimisticUpdate(11L)).thenReturn(Optional.empty());
        UpdateBalanceRequest request = new UpdateBalanceRequest(11L, BigDecimal.ONE);
        ThrowingCallable action = () -> accountService.updateBalancePessimistic(request);

        assertThatThrownBy(action)
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessageContaining("client id=11");
    }

    @Test
    void getByClientIdReturnsMappedResponse() {
        Client client = Client.builder().id(11L).firstName("A").lastName("B").phone("+37060000001").build();
        Account account = Account.builder()
                .id(22L)
                .client(client)
                .balance(new BigDecimal("10.00"))
                .version(1L)
                .build();
        AccountResponse response = new AccountResponse(22L, 11L, new BigDecimal("10.00"));

        when(accountRepository.findByClientId(11L)).thenReturn(Optional.of(account));
        when(accountMapper.toResponse(account)).thenReturn(response);

        assertThat(accountService.getByClientId(11L)).isEqualTo(response);
    }

    @Test
    void safeUpdateRetriesOnOptimisticConflictAndEventuallySucceeds() {
        AccountService spyService = spy(new AccountService(accountRepository, accountMapper, transactionManager));
        BigDecimal amount = new BigDecimal("5.00");
        AccountResponse expected = new AccountResponse(22L, 11L, new BigDecimal("15.00"));

        doThrow(new ObjectOptimisticLockingFailureException(Account.class, 11L))
                .doThrow(new OptimisticLockException())
                .doReturn(expected)
                .when(spyService).updateBalanceOptimisticTx(11L, amount);

        AccountResponse actual = spyService.safeUpdate(11L, amount);

        assertThat(actual).isEqualTo(expected);
        verify(spyService, times(3)).updateBalanceOptimisticTx(11L, amount);
    }

    @Test
    void safeUpdateThrowsAfterMaxRetries() {
        AccountService spyService = spy(new AccountService(accountRepository, accountMapper, transactionManager));
        BigDecimal amount = new BigDecimal("5.00");

        doThrow(new ObjectOptimisticLockingFailureException(Account.class, 11L))
                .when(spyService).updateBalanceOptimisticTx(11L, amount);

        assertThatThrownBy(() -> spyService.safeUpdate(11L, amount))
                .isInstanceOf(AccountOptimisticLockException.class)
                .hasMessageContaining("client id=11");
        verify(spyService, times(3)).updateBalanceOptimisticTx(11L, amount);
    }

    @Test
    void safeUpdateRethrowsNonOptimisticException() {
        AccountService spyService = spy(new AccountService(accountRepository, accountMapper, transactionManager));
        BigDecimal amount = new BigDecimal("5.00");

        doThrow(new IllegalStateException("boom"))
                .when(spyService).updateBalanceOptimisticTx(11L, amount);

        assertThatThrownBy(() -> spyService.safeUpdate(11L, amount))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("boom");
    }

    @Test
    void updateBalanceOptimisticDelegatesToSafeUpdate() {
        AccountService spyService = spy(new AccountService(accountRepository, accountMapper, transactionManager));
        UpdateBalanceRequest request = new UpdateBalanceRequest(11L, new BigDecimal("3.00"));
        AccountResponse expected = new AccountResponse(22L, 11L, new BigDecimal("13.00"));

        doReturn(expected).when(spyService).safeUpdate(11L, new BigDecimal("3.00"));

        assertThat(spyService.updateBalanceOptimistic(request)).isEqualTo(expected);
    }

    @Test
    void updateBalanceOptimisticTxThrowsWhenAccountMissing() {
        when(accountRepository.findByClientId(11L)).thenReturn(Optional.empty());
        BigDecimal amount = new BigDecimal("1.00");
        ThrowingCallable action = () -> accountService.updateBalanceOptimisticTx(11L, amount);

        assertThatThrownBy(action)
                .isInstanceOf(AccountNotFoundException.class);
        verify(accountRepository, never()).saveAndFlush(any(Account.class));
    }
}
