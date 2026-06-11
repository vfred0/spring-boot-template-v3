package com.template.data.daos;

import com.template.data.entities.core.rbac.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    @Query("select a from Account a join fetch a.client where a.client.id = :clientId")
    Optional<Account> findByClientId(@Param("clientId") Long clientId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a join fetch a.client where a.client.id = :clientId")
    Optional<Account> findByClientIdForPessimisticUpdate(@Param("clientId") Long clientId);
}
