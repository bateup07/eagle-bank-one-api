package com.eaglebank.model;

import com.eaglebank.account.AccountType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Provides validated immutable API request data.
 *
 * @author mattbateup
 */
@JsonIgnoreProperties(ignoreUnknown = false)
public record CreateAccountRequest(
        @NotBlank @Size(max = 200) String name,
        @NotNull AccountType accountType
) {
}
