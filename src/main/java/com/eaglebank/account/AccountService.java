package com.eaglebank.account;

import com.eaglebank.common.BankRules;
import com.eaglebank.common.CurrencyCode;
import com.eaglebank.common.IdGenerator;
import com.eaglebank.common.error.ApiException;
import com.eaglebank.model.AccountResponse;
import com.eaglebank.model.CreateAccountRequest;
import com.eaglebank.model.ListAccountsResponse;
import com.eaglebank.model.UpdateAccountRequest;
import com.eaglebank.user.UserEntity;
import com.eaglebank.user.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Manages owned bank accounts and their lifecycle within database transactions.
 *
 * @author mattbateup
 */
@Service
public class AccountService {

    private final AccountRepository accounts;
    private final UserRepository users;
    private final AccountAccess access;
    private final IdGenerator ids;
    private final Clock clock;

    public AccountService(
            AccountRepository accounts,
            UserRepository users,
            AccountAccess access,
            IdGenerator ids,
            Clock clock) {
        this.accounts = accounts;
        this.users = users;
        this.access = access;
        this.ids = ids;
        this.clock = clock;
    }

    /**
     * Creates a GBP account with a zero balance for the authenticated user.
     */
    @Transactional
    public AccountResponse create(String actorId, CreateAccountRequest request) {
        UserEntity user = users.findByIdForUpdate(actorId)
                .orElseThrow(() -> ApiException.unauthorized("Access token is missing or invalid"));
        Instant now = clock.instant();
        AccountEntity account = new AccountEntity();
        account.setAccountNumber(newAccountNumber());
        account.setSortCode(BankRules.SORT_CODE);
        account.setName(request.name().trim());
        account.setAccountType(request.accountType());
        account.setBalance(BankRules.ZERO);
        account.setCurrency(CurrencyCode.GBP);
        account.setUserId(user.getId());
        account.setCreatedTimestamp(now);
        account.setUpdatedTimestamp(now);
        accounts.save(account);
        return toResponse(account);
    }

    /**
     * Lists active accounts belonging to the authenticated user.
     */
    @Transactional(readOnly = true)
    public ListAccountsResponse list(String actorId) {
        List<AccountResponse> body = accounts.findAccountByUserId(actorId).stream()
                .map(AccountService::toResponse)
                .toList();
        return new ListAccountsResponse(body);
    }

    /**
     * Returns an active account after checking ownership.
     */
    @Transactional(readOnly = true)
    public AccountResponse get(String actorId, String accountNumber) {
        return toResponse(access.requireOwned(actorId, accountNumber));
    }

    /**
     * Updates supplied account fields under a database row lock.
     */
    @Transactional
    public AccountResponse update(String actorId, String accountNumber, UpdateAccountRequest request) {
        AccountEntity account = access.lockOwned(actorId, accountNumber);
        if (request.name() != null) {
            account.setName(request.name().trim());
        }
        if (request.accountType() != null) {
            account.setAccountType(request.accountType());
        }
        if (request.name() != null || request.accountType() != null) {
            account.setUpdatedTimestamp(clock.instant());
        }
        return toResponse(account);
    }

    /**
     * Soft deletes an owned account while preserving its transaction history.
     */
    @Transactional
    public void delete(String actorId, String accountNumber) {
        AccountEntity account = access.lockOwned(actorId, accountNumber);
        Instant now = clock.instant();
        account.setDeletedAt(now);
        account.setUpdatedTimestamp(now);
    }

    private String newAccountNumber() {
        for (int attempt = 0; attempt < 10; attempt++) {
            String candidate = ids.accountNumber();
            if (!accounts.existsById(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Could not allocate an account number");
    }

    static AccountResponse toResponse(AccountEntity account) {
        return new AccountResponse(
                account.getAccountNumber(),
                account.getSortCode(),
                account.getName(),
                account.getAccountType(),
                BankRules.scale(account.getBalance()),
                account.getCurrency(),
                account.getCreatedTimestamp(),
                account.getUpdatedTimestamp());
    }
}
