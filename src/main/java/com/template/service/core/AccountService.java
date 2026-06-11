package com.template.service.core;

import jakarta.persistence.OptimisticLockException;
import com.template.api.dtos.account.AccountResponse;
import com.template.api.dtos.account.UpdateBalanceRequest;
import com.template.api.http_errors.exceptions.AccountNotFoundException;
import com.template.api.http_errors.exceptions.AccountOptimisticLockException;
import com.template.config.mapper.AccountMapper;
import com.template.data.entities.core.rbac.Account;
import com.template.data.daos.AccountRepository;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.Objects;

@Service
public class AccountService {
    private static final int MAX_OPTIMISTIC_RETRIES = 3;

    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;
    private final TransactionTemplate transactionTemplate;

    public AccountService(AccountRepository accountRepository,
                          AccountMapper accountMapper,
                          PlatformTransactionManager transactionManager) {
        this.accountRepository = accountRepository;
        this.accountMapper = accountMapper;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Transactional
    public AccountResponse updateBalancePessimistic(UpdateBalanceRequest request) {
        Account account = accountRepository.findByClientIdForPessimisticUpdate(request.clientId())
                .orElseThrow(() -> new AccountNotFoundException(request.clientId()));

        account.updateBalance(account.getBalance().add(request.amount()));
        Account saved = accountRepository.saveAndFlush(account);

        return accountMapper.toResponse(saved);
    }

    public AccountResponse updateBalanceOptimistic(UpdateBalanceRequest request) {
        return safeUpdate(request.clientId(), request.amount());
    }

    public AccountResponse getByClientId(Long clientId) {
        Account account = accountRepository.findByClientId(clientId)
                .orElseThrow(() -> new AccountNotFoundException(clientId));
        return accountMapper.toResponse(account);
    }

    public AccountResponse safeUpdate(Long clientId, BigDecimal amount) {
        for (int i = 0; i < MAX_OPTIMISTIC_RETRIES; i++) {
            try {
                return Objects.requireNonNull(
                        transactionTemplate.execute(status -> updateBalanceOptimisticTx(clientId, amount)),
                        "Transaction returned null result"
                );
            } catch (RuntimeException ex) {
                if (!isOptimisticConflict(ex)) {
                    throw ex;
                }

                if (i == MAX_OPTIMISTIC_RETRIES - 1) {
                    throw new AccountOptimisticLockException(clientId);
                }
            }
        }

        throw new AccountOptimisticLockException(clientId);
    }

    protected AccountResponse updateBalanceOptimisticTx(Long clientId, BigDecimal amount) {
        Account account = accountRepository.findByClientId(clientId)
                .orElseThrow(() -> new AccountNotFoundException(clientId));

        account.updateBalance(account.getBalance().add(amount));
        Account saved = accountRepository.saveAndFlush(account);

        return accountMapper.toResponse(saved);
    }

    private boolean isOptimisticConflict(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof ObjectOptimisticLockingFailureException
                    || current instanceof OptimisticLockException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
