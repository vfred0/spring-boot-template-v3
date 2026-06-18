package com.template.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import com.template.config.RateLimitProperties;
import com.template.api.dtos.core.ApiResult;
import com.template.api.http_errors.ApiErrorType;
import com.template.service.core.shared.MessageService;
import com.template.service.core.rbac.SecurityService;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final String RATE_LIMIT_TITLE_KEY = "api.error.title.tooManyRequests";
    private static final String RATE_LIMIT_MESSAGE_KEY = "api.error.tooManyRequests";
    public static final String DEFAULT_RATE_LIMIT_BUCKETS_CACHE = "rateLimitBuckets";

    private final SecurityService securityService;
    private final MessageService messageService;
    private final RateLimitProperties rateLimitProperties;
    private final CacheManager cacheManager;
    private final ObjectMapper objectMapper;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getServletPath();

        for (RateLimitProperties.Rule rule : sortedRules()) {
            boolean shouldLimit = matchesRequest(rule, request, path)
                    && matchesClient(rule)
                    && isRateLimited(rule, resolveKey(rule, request));

            if (shouldLimit) {
                writeRateLimitedResponse(response);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private List<RateLimitProperties.Rule> sortedRules() {
        return rateLimitProperties.getRules().stream()
                .filter(RateLimitProperties.Rule::isEnabled)
                .sorted(Comparator.comparingInt(RateLimitProperties.Rule::getOrder)
                        .thenComparing(RateLimitProperties.Rule::getId))
                .toList();
    }

    private boolean matchesRequest(RateLimitProperties.Rule rule, HttpServletRequest request, String path) {
        return matchesPath(rule, path) && matchesMethod(rule, request.getMethod());
    }

    private boolean matchesPath(RateLimitProperties.Rule rule, String path) {
        return path != null && pathMatcher.match(rule.getPathPattern(), path);
    }

    private boolean matchesMethod(RateLimitProperties.Rule rule, String method) {
        Set<String> methods = rule.getMethods();
        if (methods == null || methods.isEmpty()) {
            return true;
        }
        if (!StringUtils.hasText(method)) {
            return false;
        }

        String normalizedMethod = method.trim().toUpperCase(Locale.ROOT);
        return methods.stream()
                .map(candidate -> candidate == null ? "" : candidate.trim().toUpperCase(Locale.ROOT))
                .anyMatch(candidate -> "*".equals(candidate) || candidate.equals(normalizedMethod));
    }

    private boolean matchesClient(RateLimitProperties.Rule rule) {
        Set<String> clientIds = rule.getClientIds();
        if (clientIds == null || clientIds.isEmpty()) {
            return true;
        }
        return clientIds.contains(safeValue(securityService.clientId()));
    }

    private boolean isRateLimited(RateLimitProperties.Rule rule, String key) {
        return !resolveBucket(rule, key).tryConsume(1);
    }

    private Bucket resolveBucket(RateLimitProperties.Rule rule, String key) {
        String cacheName = resolveCacheName(rule);
        Cache cache = cacheManager.getCache(cacheName);
        if (cache == null) {
            throw new IllegalStateException("Cache '" + cacheName + "' is not configured");
        }

        String cacheKey = rule.getId() + ":" + key;
        return cache.get(cacheKey, () -> Bucket.builder()
                .addLimit(Bandwidth.classic(
                        rule.getCapacity(),
                        Refill.intervally(rule.getCapacity(), Duration.ofSeconds(rule.getWindowSeconds()))
                ))
                .build()
        );
    }

    private String resolveCacheName(RateLimitProperties.Rule rule) {
        return StringUtils.hasText(rule.getCacheName())
                ? rule.getCacheName()
                : DEFAULT_RATE_LIMIT_BUCKETS_CACHE;
    }

    private String resolveKey(RateLimitProperties.Rule rule, HttpServletRequest request) {
        return switch (rule.getKeyStrategy()) {
            case IP -> "ip:" + safeValue(request.getRemoteAddr());
            case CLIENT_ID -> "client:" + safeValue(securityService.clientId());
            case USERNAME -> "user:" + safeValue(securityService.username());
            case HEADER -> "header:" + safeValue(request.getHeader(rule.getKeyHeader()));
            case CLIENT_ID_AND_IP -> "client-ip:" + safeValue(securityService.clientId()) + "|" + safeValue(request.getRemoteAddr());
        };
    }

    private String safeValue(String value) {
        return StringUtils.hasText(value) ? value : "unknown";
    }

    public void clearBuckets() {
        rateLimitProperties.getRules().stream()
                .map(this::resolveCacheName)
                .distinct()
                .forEach(cacheName -> {
                    Cache cache = cacheManager.getCache(cacheName);
                    if (cache != null) {
                        cache.clear();
                    }
                });
    }

    private void writeRateLimitedResponse(HttpServletResponse response) throws IOException {
        ApiResult<Void> error = ApiResult.error(
                ApiErrorType.TOO_MANY_REQUESTS,
                messageService.getMessage(RATE_LIMIT_TITLE_KEY),
                messageService.getMessage(RATE_LIMIT_MESSAGE_KEY)
        );

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(error));
    }
}



