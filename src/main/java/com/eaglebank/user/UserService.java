package com.eaglebank.user;

import com.eaglebank.account.AccountRepository;
import com.eaglebank.common.BankRules;
import com.eaglebank.common.IdGenerator;
import com.eaglebank.common.error.ApiException;
import com.eaglebank.model.AddressRequest;
import com.eaglebank.model.AddressResponse;
import com.eaglebank.model.CreateUserRequest;
import com.eaglebank.model.UpdateUserRequest;
import com.eaglebank.model.UserResponse;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates, updates and deletes users within transactional ownership checks.
 *
 * @author mattbateup
 */
@Service
public class UserService {

    private final UserRepository users;
    private final AccountRepository accounts;
    private final PasswordEncoder passwordEncoder;
    private final IdGenerator ids;
    private final Clock clock;

    public UserService(
            UserRepository users,
            AccountRepository accounts,
            PasswordEncoder passwordEncoder,
            IdGenerator ids,
            Clock clock) {
        this.users = users;
        this.accounts = accounts;
        this.passwordEncoder = passwordEncoder;
        this.ids = ids;
        this.clock = clock;
    }

    /**
     * Creates a user with a normalized email and hashed password.
     */
    @Transactional
    public UserResponse create(CreateUserRequest request) {
        String email = normalizeEmail(request.email());
        if (users.isEmailAddressExists(email)) {
            throw ApiException.conflict("A user with this email already exists");
        }
        Instant now = clock.instant();
        UserEntity user = new UserEntity();
        user.setId(ids.userId());
        user.setName(request.name().trim());
        user.setAddress(toAddress(request.address()));
        user.setPhoneNumber(request.phoneNumber().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setCreatedTimestamp(now);
        user.setUpdatedTimestamp(now);
        try {
            users.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            throw ApiException.conflict("A user with this email already exists");
        }
        return toResponse(user);
    }

    /**
     * Returns the requested user after checking ownership.
     */
    @Transactional(readOnly = true)
    public UserResponse get(String actorId, String userId) {
        UserEntity user = users.findById(userId)
                .orElseThrow(() -> ApiException.notFound("User was not found"));
        requireOwner(actorId, user, "access");
        return toResponse(user);
    }

    /**
     * Updates supplied fields while retaining omitted values.
     */
    @Transactional
    public UserResponse update(String actorId, String userId, UpdateUserRequest request) {
        UserEntity user = users.findByIdForUpdate(userId)
                .orElseThrow(() -> ApiException.notFound("User was not found"));
        requireOwner(actorId, user, "update");
        if (request.name() != null) {
            user.setName(request.name().trim());
        }
        if (request.address() != null) {
            user.setAddress(toAddress(request.address()));
        }
        if (request.phoneNumber() != null) {
            user.setPhoneNumber(request.phoneNumber().trim());
        }
        if (request.email() != null) {
            String email = normalizeEmail(request.email());
            if (!email.equals(user.getEmail()) && users.isEmailAddressExists(email)) {
                throw ApiException.conflict("A user with this email already exists");
            }
            user.setEmail(email);
        }
        if (request.name() != null || request.address() != null
                || request.phoneNumber() != null || request.email() != null) {
            user.setUpdatedTimestamp(clock.instant());
        }
        return toResponse(user);
    }

    /**
     * Deletes an owned user only when no active bank accounts remain.
     */
    @Transactional
    public void delete(String actorId, String userId) {
        UserEntity user = users.findByIdForUpdate(userId)
                .orElseThrow(() -> ApiException.notFound("User was not found"));
        requireOwner(actorId, user, "delete");
        if (accounts.isAccountExists(userId)) {
            throw ApiException.conflict("A user cannot be deleted when they are associated with a bank account");
        }
        users.delete(user);
    }

    private static void requireOwner(String actorId, UserEntity user, String action) {
        if (!user.getId().equals(actorId)) {
            throw ApiException.forbidden("You are not allowed to " + action + " this user");
        }
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static AddressEmbeddable toAddress(AddressRequest request) {
        return new AddressEmbeddable(
                request.line1().trim(),
                BankRules.blankToNull(request.line2()),
                BankRules.blankToNull(request.line3()),
                request.town().trim(),
                request.county().trim(),
                request.postcode().trim());
    }

    private static UserResponse toResponse(UserEntity user) {
        AddressEmbeddable address = user.getAddress();
        return new UserResponse(
                user.getId(),
                user.getName(),
                new AddressResponse(
                        address.getLine1(),
                        address.getLine2(),
                        address.getLine3(),
                        address.getTown(),
                        address.getCounty(),
                        address.getPostcode()),
                user.getPhoneNumber(),
                user.getEmail(),
                user.getCreatedTimestamp(),
                user.getUpdatedTimestamp());
    }
}
