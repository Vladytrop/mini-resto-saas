package com.resto.saas.shared.tenant;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TenantFilter extends OncePerRequestFilter {

    public static final String TENANT_HEADER = "X-Tenant-ID";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String tenantId = request.getHeader(TENANT_HEADER);
        log.info(">>> TenantFilter: received tenantId = '{}'", tenantId);

        if (!StringUtils.hasText(tenantId)) {
            log.warn("Missing header {} for URI: {}", TENANT_HEADER, request.getRequestURI());
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                "Required header '" + TENANT_HEADER + "' is missing");
            return;
        }

        try {
            TenantContext.setTenant(tenantId);
            log.debug("Tenant set: {}", tenantId);
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
            log.debug("Tenant cleared");
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator") || path.startsWith("/swagger");
    }
}
