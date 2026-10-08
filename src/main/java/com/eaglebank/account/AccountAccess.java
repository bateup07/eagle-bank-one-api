package com.eaglebank.account;

import com.eaglebank.common.error.ApiException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Centralizes account ownership checks and row locking for balance changes.
 *
 * @author mattbateup
 */
@Component
public class AccountAccess {

    private final AccountRepository accounts;

    public AccountAccess(AccountRepository accounts) {
        this.accounts = accounts;
    }

    /**
     * Loads an active account and verifies its owner.
     */
    @Transactional(readOnly = true)
    public AccountEntity requireOwned(String actorId, String accountNumber) {
        return owned(actorId, accounts.findAccountByAccountNumber(accountNumber)
                .orElseThrow(() -> ApiException.notFound("Bank account was not found")));
    }

    /**
     * Locks an owned active account inside the caller transaction.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public AccountEntity lockOwned(String actorId, String accountNumber) {
        return owned(actorId, accounts.findActiveForUpdate(accountNumber)
                .orElseThrow(() -> ApiException.notFound("Bank account was not found")));
    }

    private static AccountEntity owned(String actorId, AccountEntity account) {
        if (!account.getUserId().equals(actorId)) {
            throw ApiException.forbidden("You are not allowed to access this bank account");
        }
        return account;
    }
}
