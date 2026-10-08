package com.eaglebank.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Provides validated immutable API request data.
 *
 * @author mattbateup
 */
@JsonIgnoreProperties(ignoreUnknown = false)
public record LoginRequest(
        @NotBlank @Email String email,
        @NotBlank String password
) {
}
