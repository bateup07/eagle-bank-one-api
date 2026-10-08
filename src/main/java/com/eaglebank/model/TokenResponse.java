package com.eaglebank.model;

/**
 * Provides immutable API response data.
 *
 * @author mattbateup
 */
public record TokenResponse(String accessToken, String tokenType, long expiresIn) {
}
