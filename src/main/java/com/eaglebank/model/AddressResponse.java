package com.eaglebank.model;

/**
 * Provides immutable API response data.
 *
 * @author mattbateup
 */
public record AddressResponse(
        String line1,
        String line2,
        String line3,
        String town,
        String county,
        String postcode
) {
}
