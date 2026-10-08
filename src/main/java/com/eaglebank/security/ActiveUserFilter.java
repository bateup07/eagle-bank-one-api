package com.eaglebank.security;

import com.eaglebank.user.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Rejects access tokens whose subject no longer identifies an active user.
 *
 * @author mattbateup
 */
public class ActiveUserFilter extends OncePerRequestFilter {

    private static final String USER_ID = "^usr-[A-Za-z0-9]{1,32}$";

    private final UserRepository users;
    private final SecurityErrorWriter errors;

    public ActiveUserFilter(UserRepository users, SecurityErrorWriter errors) {
        this.users = users;
        this.errors = errors;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return "/error".equals(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            String subject = jwt.getSubject();
            if (subject == null || !subject.matches(USER_ID) || !users.existsById(subject)) {
                SecurityContextHolder.clearContext();
                errors.unauthorized(response);
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
