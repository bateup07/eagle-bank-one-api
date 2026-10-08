package com.eaglebank.account;

import com.eaglebank.common.CurrentUser;
import com.eaglebank.model.AccountResponse;
import com.eaglebank.model.CreateAccountRequest;
import com.eaglebank.model.ListAccountsResponse;
import com.eaglebank.model.UpdateAccountRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
@RequestMapping(path = "/v1/accounts", produces = MediaType.APPLICATION_JSON_VALUE)
public class AccountController {

    public static final String ACCOUNT_NUMBER = "^01\\d{6}$";

    private final AccountService accounts;

    public AccountController(AccountService accounts) {
        this.accounts = accounts;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateAccountRequest request) {
        return accounts.create(CurrentUser.id(jwt), request);
    }

    @GetMapping
    public ListAccountsResponse list(@AuthenticationPrincipal Jwt jwt) {
        return accounts.list(CurrentUser.id(jwt));
    }

    @GetMapping("/{accountNumber}")
    public AccountResponse get(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable @Pattern(regexp = ACCOUNT_NUMBER, message = "must be an account number such as 01234567") String accountNumber) {
        return accounts.get(CurrentUser.id(jwt), accountNumber);
    }

    @PatchMapping(path = "/{accountNumber}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public AccountResponse update(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable @Pattern(regexp = ACCOUNT_NUMBER, message = "must be an account number such as 01234567") String accountNumber,
            @Valid @RequestBody UpdateAccountRequest request) {
        return accounts.update(CurrentUser.id(jwt), accountNumber, request);
    }

    @DeleteMapping("/{accountNumber}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable @Pattern(regexp = ACCOUNT_NUMBER, message = "must be an account number such as 01234567") String accountNumber) {
        accounts.delete(CurrentUser.id(jwt), accountNumber);
    }
}
