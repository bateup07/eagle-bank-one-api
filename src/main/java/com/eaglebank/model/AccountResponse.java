package com.eaglebank.model;

import com.eaglebank.account.AccountType;
import com.eaglebank.common.CurrencyCode;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Provides immutable API response data.
 *
 * @author mattbateup
 */
public record AccountResponse(
        String accountNumber,
        String sortCode,
        String name,
        AccountType accountType,
        BigDecimal balance,
        CurrencyCode currency,
        Instant createdTimestamp,
        Instant updatedTimestamp
) {
}
