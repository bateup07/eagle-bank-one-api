package com.eaglebank.model;

import java.time.Instant;

/**
 * Provides immutable API response data.
 *
 * @author mattbateup
 */
public record UserResponse(
        String id,
        String name,
        AddressResponse address,
        String phoneNumber,
        String email,
        Instant createdTimestamp,
        Instant updatedTimestamp
) {
}
