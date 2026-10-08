package com.eaglebank.model;

import com.eaglebank.common.validation.NullOrNotBlank;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Provides validated immutable API request data.
 *
 * @author mattbateup
 */
@JsonIgnoreProperties(ignoreUnknown = false)
public record UpdateUserRequest(
        @NullOrNotBlank @Size(max = 200) String name,
        @Valid AddressRequest address,
        @Pattern(regexp = "^\\+[1-9]\\d{1,14}$") String phoneNumber,
        @Email @Size(max = 254) String email
) {
}
