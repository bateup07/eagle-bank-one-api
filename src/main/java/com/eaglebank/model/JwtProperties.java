package com.eaglebank.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Provides support for jwt properties.
 *
 * @author mattbateup
 */
@Validated
@ConfigurationProperties(prefix = "eaglebank.jwt")
public record JwtProperties(
        @NotBlank String secret,
        @Positive long ttlSeconds,
        @NotBlank String issuer
) {
}
