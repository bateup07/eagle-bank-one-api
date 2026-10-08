package com.eaglebank.model;

import com.eaglebank.account.AccountType;
import com.eaglebank.common.validation.NullOrNotBlank;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Size;

/**
 * Provides validated immutable API request data.
 *
 * @author mattbateup
 */
@JsonIgnoreProperties(ignoreUnknown = false)
public record UpdateAccountRequest(
        @NullOrNotBlank @Size(max = 200) String name,
        AccountType accountType
) {
}
