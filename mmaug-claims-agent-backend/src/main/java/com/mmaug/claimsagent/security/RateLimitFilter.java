package com.mmaug.claimsagent.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int UPLOAD_LIMIT_PER_MINUTE = 10;
    private static final int ANALYSE_LIMIT_PER_MINUTE = 5;

    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        String userKey = request.getRemoteAddr();

        if (path.matches("/api/v1/claims/[^/]+/documents") && "POST".equals(request.getMethod())) {
            if (!allow(userKey + ":upload", UPLOAD_LIMIT_PER_MINUTE)) {
                response.setStatus(429);
                response.setContentType("application/json");
                response.getWriter().write("{\"code\":\"TOO_MANY_REQUESTS\",\"message\":\"Upload rate limit exceeded\"}");
                return;
            }
        } else if (path.matches("/api/v1/claims/[^/]+/analyse") && "POST".equals(request.getMethod())) {
            if (!allow(userKey + ":analyse", ANALYSE_LIMIT_PER_MINUTE)) {
                response.setStatus(429);
                response.setContentType("application/json");
                response.getWriter().write("{\"code\":\"TOO_MANY_REQUESTS\",\"message\":\"Analyse rate limit exceeded\"}");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private boolean allow(String key, int limit) {
        Window window = windows.computeIfAbsent(key, k -> new Window());
        return window.tryConsume(limit);
    }

    private static final class Window {
        private final AtomicInteger count = new AtomicInteger(0);
        private volatile Instant windowStart = Instant.now();

        synchronized boolean tryConsume(int limit) {
            Instant now = Instant.now();
            if (now.isAfter(windowStart.plusSeconds(60))) {
                windowStart = now;
                count.set(0);
            }
            return count.incrementAndGet() <= limit;
        }
    }
}