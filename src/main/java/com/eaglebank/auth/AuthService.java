package com.eaglebank.auth;

import com.eaglebank.common.error.ApiException;
import com.eaglebank.model.LoginRequest;
import com.eaglebank.user.UserEntity;
import com.eaglebank.user.UserRepository;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Verifies credentials using stored password hashes without exposing user existence.
 *
 * @author mattbateup
 */
@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final String dummyHash;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.dummyHash = passwordEncoder.encode("dummy-password-not-used");
    }

    /**
     * Returns the authenticated user ID, or rejects invalid credentials.
     */
    public String authenticate(LoginRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        UserEntity user = users.findByEmail(email).orElse(null);
        String hash = user == null ? dummyHash : user.getPasswordHash();
        boolean validPassword = passwordEncoder.matches(request.password(), hash);
        if (user == null || !validPassword) {
            throw ApiException.unauthorized("Invalid email or password");
        }
        return user.getId();
    }
}
