package com.tecspace.portal.security;

import com.tecspace.portal.model.AuditLog;
import com.tecspace.portal.repository.AuditLogRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/** Records who changed what: every POST, PUT, DELETE (never request bodies, so no passwords are stored). */
public class AuditFilter extends OncePerRequestFilter {

    private static final Set<String> CHANGING = Set.of("POST", "PUT", "DELETE", "PATCH");

    private final AuditLogRepository logs;

    public AuditFilter(AuditLogRepository logs) {
        this.logs = logs;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        if (!CHANGING.contains(request.getMethod()) || !path.startsWith("/api/") || path.startsWith("/api/auth/")) {
            chain.doFilter(request, response);
            return;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String actor = auth == null ? "anonymous" : auth.getName();
        String role = (auth == null || auth.getAuthorities().isEmpty()) ? "-" : auth.getAuthorities().iterator().next().getAuthority();

        try {
            chain.doFilter(request, response);
        } finally {
            try {
                AuditLog log = new AuditLog();
                log.setActor(actor);
                log.setRole(role);
                log.setMethod(request.getMethod());
                log.setPath(path);
                log.setStatus(response.getStatus());
                log.setIp(request.getRemoteAddr());
                logs.save(log);
            } catch (Exception ignored) {
                // never let logging break the request
            }
        }
    }
}
