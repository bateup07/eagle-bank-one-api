package com.eaglebank.transaction;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Provides persistence queries for the domain.
 *
 * @author mattbateup
 */
public interface TransactionRepository extends JpaRepository<TransactionEntity, String> {

    List<TransactionEntity> findTransactionByAccountNumber(String accountNumber);

    Optional<TransactionEntity> findTransactionByIdAndAccountNumber(String id, String accountNumber);
}
