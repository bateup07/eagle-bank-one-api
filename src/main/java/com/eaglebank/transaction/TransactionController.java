package com.eaglebank.transaction;

import com.eaglebank.account.AccountController;
import com.eaglebank.common.CurrentUser;
import com.eaglebank.model.CreateTransactionRequest;
import com.eaglebank.model.ListTransactionsResponse;
import com.eaglebank.model.TransactionResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Provides HTTP operations and request validation.
 *
 * @author mattbateup
 */
@RestController
@Validated
@RequestMapping(path = "/v1/accounts/{accountNumber}/transactions", produces = MediaType.APPLICATION_JSON_VALUE)
public class TransactionController {

    private static final String TRANSACTION_ID = "^tan-[A-Za-z0-9]{1,32}$";

    private final TransactionService transactions;

    public TransactionController(TransactionService transactions) {
        this.transactions = transactions;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse create(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable @Pattern(regexp = AccountController.ACCOUNT_NUMBER, message = "must be an account number such as 01234567") String accountNumber,
            @Valid @RequestBody CreateTransactionRequest request) {
        return transactions.create(CurrentUser.id(jwt), accountNumber, request);
    }

    @GetMapping
    public ListTransactionsResponse list(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable @Pattern(regexp = AccountController.ACCOUNT_NUMBER, message = "must be an account number such as 01234567") String accountNumber) {
        return transactions.list(CurrentUser.id(jwt), accountNumber);
    }

    @GetMapping("/{transactionId}")
    public TransactionResponse get(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable @Pattern(regexp = AccountController.ACCOUNT_NUMBER, message = "must be an account number such as 01234567") String accountNumber,
            @PathVariable @Pattern(regexp = TRANSACTION_ID, message = "must be a transaction id such as tan-123abc") String transactionId) {
        return transactions.get(CurrentUser.id(jwt), accountNumber, transactionId);
    }
}
