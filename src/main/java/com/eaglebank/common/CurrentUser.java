package com.eaglebank.common;

import com.eaglebank.common.error.ApiException;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Extracts the authenticated user ID from the Spring Security principal.
 *
 * @author mattbateup
 */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static String id(Jwt jwt) {
        if (jwt == null || jwt.getSubject() == null) {
            throw ApiException.unauthorized("Access token is missing or invalid");
        }
        return jwt.getSubject();
    }
}
