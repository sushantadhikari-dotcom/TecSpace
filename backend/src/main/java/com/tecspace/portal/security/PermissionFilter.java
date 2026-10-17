package com.tecspace.portal.security;

import com.tecspace.portal.model.User;
import com.tecspace.portal.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Runs after the JWT is read. It (1) checks against the database that the account still exists and is active,
 * (2) uses the CURRENT role from the database, so a role change takes effect straight away, and
 * (3) checks the permission a staff member needs for the admin endpoint being called.
 * Not a Spring bean on purpose: it is added to the security chain in SecurityConfig.
 */
public class PermissionFilter extends OncePerRequestFilter {

    private final UserRepository users;

    public PermissionFilter(UserRepository users) {
        this.users = users;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String path = request.getRequestURI();

        if (auth != null && path.startsWith("/api/") && !isPublic(path)) {
            User user = users.findByUniversityEmail(auth.getName()).orElse(null);
            if (user == null || !user.isActive()) {
                reject(response, 401, "This account is disabled or no longer exists");
                return;
            }

            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                    user.getUniversityEmail(), null, List.of(new SimpleGrantedAuthority(user.getRole()))));

            if (path.startsWith("/api/admin/") && "ROLE_STAFF".equals(user.getRole())) {
                String needed = Permissions.required(request.getMethod(), path.substring("/api/admin/".length()));
                boolean allowed = Permissions.ANY_STAFF.equals(needed)
                        || (needed != null && user.getPermissions().contains(needed));
                if (!allowed) {
                    reject(response, 403, "You do not have permission to do this");
                    return;
                }
            }
        }
        chain.doFilter(request, response);
    }

    private boolean isPublic(String path) {
        return path.startsWith("/api/auth/") || path.startsWith("/api/pay/") || path.startsWith("/api/public/");
    }

    private void reject(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"message\":\"" + message + "\"}");
    }
}
