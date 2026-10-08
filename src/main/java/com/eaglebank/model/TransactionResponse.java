package com.eaglebank.model;

import com.eaglebank.common.CurrencyCode;
import com.eaglebank.transaction.TransactionType;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Provides immutable API response data.
 *
 * @author mattbateup
 */
public record TransactionResponse(
        String id,
        BigDecimal amount,
        CurrencyCode currency,
        TransactionType type,
        String reference,
        String userId,
        Instant createdTimestamp
) {
}
