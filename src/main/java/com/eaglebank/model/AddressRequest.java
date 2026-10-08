package com.eaglebank.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Provides validated immutable API request data.
 *
 * @author mattbateup
 */
@JsonIgnoreProperties(ignoreUnknown = false)
public record AddressRequest(
        @NotBlank @Size(max = 255) String line1,
        @Size(max = 255) String line2,
        @Size(max = 255) String line3,
        @NotBlank @Size(max = 100) String town,
        @NotBlank @Size(max = 100) String county,
        @NotBlank @Size(max = 32) String postcode
) {
}
