package com.eaglebank.account;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

/**
 * Provides persistence queries for the domain.
 *
 * @author mattbateup
 */
public interface AccountRepository extends JpaRepository<AccountEntity, String> {

    List<AccountEntity> findAccountByUserId(String userId);

    Optional<AccountEntity> findAccountByAccountNumber(String accountNumber);

    boolean isAccountExists(String userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "5000"))
    @Query("select account from AccountEntity account where account.accountNumber = :accountNumber and account.deletedAt is null")
    Optional<AccountEntity> findActiveForUpdate(@Param("accountNumber") String accountNumber);
}
