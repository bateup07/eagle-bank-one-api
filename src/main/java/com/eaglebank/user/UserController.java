package com.eaglebank.user;

import com.eaglebank.common.CurrentUser;
import com.eaglebank.model.CreateUserRequest;
import com.eaglebank.model.UpdateUserRequest;
import com.eaglebank.model.UserResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Provides HTTP operations and request validation.
 *
 * @author mattbateup
 */
@RestController
@Validated
@RequestMapping(path = "/v1/users", produces = MediaType.APPLICATION_JSON_VALUE)
public class UserController {

    private static final String USER_ID = "^usr-[A-Za-z0-9]{1,32}$";

    private final UserService users;

    public UserController(UserService users) {
        this.users = users;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@Valid @RequestBody CreateUserRequest request) {
        return users.create(request);
    }

    @GetMapping("/{userId}")
    public UserResponse get(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable @Pattern(regexp = USER_ID, message = "must be a user id such as usr-abc123") String userId) {
        return users.get(CurrentUser.id(jwt), userId);
    }

    @PatchMapping(path = "/{userId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public UserResponse update(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable @Pattern(regexp = USER_ID, message = "must be a user id such as usr-abc123") String userId,
            @Valid @RequestBody UpdateUserRequest request) {
        return users.update(CurrentUser.id(jwt), userId, request);
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable @Pattern(regexp = USER_ID, message = "must be a user id such as usr-abc123") String userId) {
        users.delete(CurrentUser.id(jwt), userId);
    }
}
