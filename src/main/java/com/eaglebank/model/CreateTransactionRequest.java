package com.eaglebank.model;

import com.eaglebank.common.CurrencyCode;
import com.eaglebank.common.validation.MoneyAmount;
import com.eaglebank.transaction.TransactionType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Provides validated immutable API request data.
 *
 * @author mattbateup
 */
@JsonIgnoreProperties(ignoreUnknown = false)
public record CreateTransactionRequest(
        @NotNull @MoneyAmount BigDecimal amount,
        @NotNull CurrencyCode currency,
        @NotNull TransactionType type,
        @Size(max = 255) String reference
) {
}
