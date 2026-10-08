package com.eaglebank.transaction;

import com.eaglebank.account.AccountAccess;
import com.eaglebank.account.AccountEntity;
import com.eaglebank.common.BankRules;
import com.eaglebank.common.IdGenerator;
import com.eaglebank.common.error.ApiException;
import com.eaglebank.model.CreateTransactionRequest;
import com.eaglebank.model.ListTransactionsResponse;
import com.eaglebank.model.TransactionResponse;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Records deposits and withdrawals atomically with the account balance update.
 *
 * @author mattbateup
 */
@Service
public class TransactionService {

    private final TransactionRepository transactions;
    private final AccountAccess accounts;
    private final IdGenerator ids;
    private final Clock clock;

    public TransactionService(TransactionRepository transactions, AccountAccess accounts, IdGenerator ids, Clock clock) {
        this.transactions = transactions;
        this.accounts = accounts;
        this.ids = ids;
        this.clock = clock;
    }

    /**
     * Records a transaction and balance change atomically under an account lock.
     */
    @Transactional
    public TransactionResponse create(String actorId, String accountNumber, CreateTransactionRequest request) {
        AccountEntity account = accounts.lockOwned(actorId, accountNumber);
        BigDecimal amount = BankRules.scale(request.amount());
        BigDecimal updated = switch (request.type()) {
            case deposit -> account.getBalance().add(amount);
            case withdrawal -> account.getBalance().subtract(amount);
        };
        if (updated.signum() < 0) {
            throw ApiException.unprocessable("Insufficient funds to process transaction");
        }
        if (updated.compareTo(BankRules.MAX_BALANCE) > 0) {
            throw ApiException.unprocessable("Account balance cannot exceed 10000.00");
        }
        account.setBalance(BankRules.scale(updated));
        account.setUpdatedTimestamp(clock.instant());

        TransactionEntity transaction = new TransactionEntity();
        transaction.setId(ids.transactionId());
        transaction.setAccountNumber(account.getAccountNumber());
        transaction.setAmount(amount);
        transaction.setCurrency(request.currency());
        transaction.setType(request.type());
        transaction.setReference(BankRules.blankToNull(request.reference()));
        transaction.setUserId(actorId);
        transaction.setCreatedTimestamp(clock.instant());
        transactions.save(transaction);
        return toResponse(transaction);
    }

    /**
     * Lists transactions after checking account ownership.
     */
    @Transactional(readOnly = true)
    public ListTransactionsResponse list(String actorId, String accountNumber) {
        accounts.requireOwned(actorId, accountNumber);
        List<TransactionResponse> body = transactions.findTransactionByAccountNumber(accountNumber).stream()
                .map(TransactionService::toResponse)
                .toList();
        return new ListTransactionsResponse(body);
    }

    /**
     * Returns a transaction belonging to the requested owned account.
     */
    @Transactional(readOnly = true)
    public TransactionResponse get(String actorId, String accountNumber, String transactionId) {
        accounts.requireOwned(actorId, accountNumber);
        return transactions.findTransactionByIdAndAccountNumber(transactionId, accountNumber)
                .map(TransactionService::toResponse)
                .orElseThrow(() -> ApiException.notFound("Transaction was not found"));
    }

    private static TransactionResponse toResponse(TransactionEntity transaction) {
        return new TransactionResponse(
                transaction.getId(),
                BankRules.scale(transaction.getAmount()),
                transaction.getCurrency(),
                transaction.getType(),
                transaction.getReference(),
                transaction.getUserId(),
                transaction.getCreatedTimestamp());
    }
}
