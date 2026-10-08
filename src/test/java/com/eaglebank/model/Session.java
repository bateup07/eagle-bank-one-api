package com.eaglebank.model;

/**
 * Provides support for session.
 *
 * @author mattbateup
 */
public record Session(String userId, String accessToken) {
    public String bearer() {
        return "Bearer " + accessToken;
    }
}
