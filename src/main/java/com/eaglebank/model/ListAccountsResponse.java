package com.eaglebank.model;

import java.util.List;

/**
 * Provides immutable API response data.
 *
 * @author mattbateup
 */
public record ListAccountsResponse(List<AccountResponse> accounts) {
}
