package org.scribereel.filter;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.scribereel.config.AppPropertiesConfig;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RateLimitFilter extends HttpFilter {
    private final Cache<String, AtomicInteger> requestCounts;
    private final AppPropertiesConfig appProperties;

    public RateLimitFilter(AppPropertiesConfig appProperties) {
        this.appProperties = appProperties;
        this.requestCounts = Caffeine.newBuilder()
                .expireAfterWrite(appProperties.getRateLimitWindowMinutes(), TimeUnit.MINUTES)
                .maximumSize(10_000) // hard ceiling so a spoofed-IP flood can't grow this unbounded either
                .build();
    }

    @Override
    protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws IOException, ServletException {

        Integer maxRequests = resolveLimit(request);
        if (maxRequests == null) {
            chain.doFilter(request, response);
            return;
        }

        String key = clientIp(request) + ":" + request.getRequestURI();
        AtomicInteger count = requestCounts.get(key, k -> new AtomicInteger(0));

        if (count.incrementAndGet() > maxRequests) {
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Please wait before submitting another request.\"}");
            return;
        }

        chain.doFilter(request, response);
    }

    /** Returns the per-route max request count, or null if this route isn't rate-limited. */
    private Integer resolveLimit(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return null;
        }
        String uri = request.getRequestURI();
        return switch (uri) {
            case "/api/caption", "/api/transcribe" -> appProperties.getRateLimitMaxRequests();
            case "/api/convert" -> appProperties.getRateLimitMaxRequestsConvert();
            default -> null;
        };
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            String[] ips = forwardedFor.split(",");
            return ips[ips.length - 1].trim(); // last hop = added by your trusted proxy
        }
        return request.getRemoteAddr();
    }

}
