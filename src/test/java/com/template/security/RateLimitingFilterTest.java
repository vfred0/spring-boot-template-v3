package com.template.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import com.template.config.RateLimitProperties;
import com.template.config.security.RateLimitingFilter;
import com.template.service.core.shared.MessageService;
import com.template.service.core.rbac.SecurityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateLimitingFilterTest {

    @Mock
    private SecurityService securityService;

    @Mock
    private MessageService messageService;

    private RateLimitProperties properties;
    private RateLimitingFilter filter;

    @BeforeEach
    void setUp() {
        properties = new RateLimitProperties();
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager(
                RateLimitingFilter.DEFAULT_RATE_LIMIT_BUCKETS_CACHE,
                "loginBuckets",
                "clientsBuckets"
        );

        filter = new RateLimitingFilter(
                securityService,
                messageService,
                properties,
                cacheManager,
                new ObjectMapper()
        );
    }

    @Test
    void allowsRequestWhenRuleDoesNotMatchPath() throws ServletException, IOException {
        properties.setRules(List.of(rule("login", "/api/auth/login", "loginBuckets", RateLimitProperties.KeyStrategy.IP, Set.of("POST"), 1)));

        MockHttpServletRequest request = request("GET", "/api/clients/123", "10.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void appliesRateLimitUsingConfiguredCacheName() throws ServletException, IOException {
        when(messageService.getMessage("api.error.tooManyRequests")).thenReturn("Too many requests");
        properties.setRules(List.of(rule("login", "/api/auth/login", "loginBuckets", RateLimitProperties.KeyStrategy.IP, Set.of("POST"), 1)));

        MockHttpServletRequest first = request("POST", "/api/auth/login", "10.0.0.1");
        MockHttpServletResponse firstResponse = new MockHttpServletResponse();
        MockFilterChain firstChain = new MockFilterChain();
        filter.doFilter(first, firstResponse, firstChain);

        MockHttpServletRequest second = request("POST", "/api/auth/login", "10.0.0.1");
        MockHttpServletResponse secondResponse = new MockHttpServletResponse();
        MockFilterChain secondChain = new MockFilterChain();
        filter.doFilter(second, secondResponse, secondChain);

        assertThat(firstChain.getRequest()).isNotNull();
        assertThat(secondChain.getRequest()).isNull();
        assertThat(secondResponse.getStatus()).isEqualTo(429);
        assertThat(secondResponse.getContentAsString()).contains("\"code\":42901");
    }

    @Test
    void usesDefaultCacheWhenRuleCacheNameIsBlank() throws ServletException, IOException {
        when(messageService.getMessage("api.error.tooManyRequests")).thenReturn("Too many requests");
        RateLimitProperties.Rule defaultCacheRule = rule("default-cache", "/api/auth/login", " ", RateLimitProperties.KeyStrategy.IP, Set.of("POST"), 1);
        properties.setRules(List.of(defaultCacheRule));

        filter.doFilter(
                request("POST", "/api/auth/login", "10.0.0.1"),
                new MockHttpServletResponse(),
                new MockFilterChain()
        );

        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(
                request("POST", "/api/auth/login", "10.0.0.1"),
                response,
                new MockFilterChain()
        );

        assertThat(response.getStatus()).isEqualTo(429);
    }

    @Test
    void throwsWhenConfiguredCacheDoesNotExist() {
        properties.setRules(List.of(rule("missing-cache", "/api/auth/login", "unknown-cache", RateLimitProperties.KeyStrategy.IP, Set.of("POST"), 1)));
        MockHttpServletRequest request = request("POST", "/api/auth/login", "10.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        assertThatThrownBy(() -> filter.doFilter(request, response, chain))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("unknown-cache");
    }

    @Test
    void clearsAllRuleCachesAndResetsLimits() throws ServletException, IOException {
        when(messageService.getMessage("api.error.tooManyRequests")).thenReturn("Too many requests");
        when(securityService.clientId()).thenReturn("spring-app");
        RateLimitProperties.Rule loginRule = rule("login", "/api/auth/login", "loginBuckets", RateLimitProperties.KeyStrategy.IP, Set.of("POST"), 1);
        RateLimitProperties.Rule clientsRule = rule("clients", "/api/clients/**", "clientsBuckets", RateLimitProperties.KeyStrategy.CLIENT_ID, Set.of("GET"), 1);
        properties.setRules(List.of(loginRule, clientsRule));

        filter.doFilter(request("POST", "/api/auth/login", "10.0.0.1"), new MockHttpServletResponse(), new MockFilterChain());
        filter.doFilter(request("GET", "/api/clients/1", "10.0.0.1"), new MockHttpServletResponse(), new MockFilterChain());

        MockHttpServletResponse blockedLogin = new MockHttpServletResponse();
        filter.doFilter(request("POST", "/api/auth/login", "10.0.0.1"), blockedLogin, new MockFilterChain());
        MockHttpServletResponse blockedClients = new MockHttpServletResponse();
        filter.doFilter(request("GET", "/api/clients/1", "10.0.0.1"), blockedClients, new MockFilterChain());

        assertThat(blockedLogin.getStatus()).isEqualTo(429);
        assertThat(blockedClients.getStatus()).isEqualTo(429);

        filter.clearBuckets();

        MockHttpServletResponse afterClearLogin = new MockHttpServletResponse();
        MockFilterChain loginChain = new MockFilterChain();
        filter.doFilter(request("POST", "/api/auth/login", "10.0.0.1"), afterClearLogin, loginChain);

        MockHttpServletResponse afterClearClients = new MockHttpServletResponse();
        MockFilterChain clientsChain = new MockFilterChain();
        filter.doFilter(request("GET", "/api/clients/1", "10.0.0.1"), afterClearClients, clientsChain);

        assertThat(afterClearLogin.getStatus()).isEqualTo(200);
        assertThat(afterClearClients.getStatus()).isEqualTo(200);
        assertThat(loginChain.getRequest()).isNotNull();
        assertThat(clientsChain.getRequest()).isNotNull();
    }

    @Test
    void supportsAllKeyStrategies() throws ServletException, IOException {
        when(messageService.getMessage("api.error.tooManyRequests")).thenReturn("Too many requests");
        when(securityService.clientId()).thenReturn("spring-app");
        when(securityService.username()).thenReturn("user-1");
        RateLimitProperties.Rule usernameRule = rule("username", "/api/u/**", "loginBuckets", RateLimitProperties.KeyStrategy.USERNAME, Set.of("GET"), 1);
        RateLimitProperties.Rule headerRule = rule("header", "/api/h/**", "clientsBuckets", RateLimitProperties.KeyStrategy.HEADER, Set.of("GET"), 1);
        headerRule.setKeyHeader("X-Rate-Key");
        RateLimitProperties.Rule clientIpRule = rule("client-ip", "/api/c/**", "clientsBuckets", RateLimitProperties.KeyStrategy.CLIENT_ID_AND_IP, Set.of("GET"), 1);

        properties.setRules(List.of(usernameRule, headerRule, clientIpRule));

        filter.doFilter(request("GET", "/api/u/1", "10.0.0.2"), new MockHttpServletResponse(), new MockFilterChain());
        MockHttpServletResponse usernameLimited = new MockHttpServletResponse();
        filter.doFilter(request("GET", "/api/u/1", "10.0.0.2"), usernameLimited, new MockFilterChain());

        MockHttpServletRequest headerFirst = request("GET", "/api/h/1", "10.0.0.2");
        headerFirst.addHeader("X-Rate-Key", "A");
        filter.doFilter(headerFirst, new MockHttpServletResponse(), new MockFilterChain());
        MockHttpServletRequest headerSecond = request("GET", "/api/h/1", "10.0.0.2");
        headerSecond.addHeader("X-Rate-Key", "A");
        MockHttpServletResponse headerLimited = new MockHttpServletResponse();
        filter.doFilter(headerSecond, headerLimited, new MockFilterChain());

        filter.doFilter(request("GET", "/api/c/1", "10.0.0.3"), new MockHttpServletResponse(), new MockFilterChain());
        MockHttpServletResponse clientIpLimited = new MockHttpServletResponse();
        filter.doFilter(request("GET", "/api/c/1", "10.0.0.3"), clientIpLimited, new MockFilterChain());

        assertThat(usernameLimited.getStatus()).isEqualTo(429);
        assertThat(headerLimited.getStatus()).isEqualTo(429);
        assertThat(clientIpLimited.getStatus()).isEqualTo(429);
    }

    @Test
    void skipsRuleWhenClientIdDoesNotMatchRestriction() throws ServletException, IOException {
        when(securityService.clientId()).thenReturn("spring-app");
        RateLimitProperties.Rule clientRestricted = rule("clients", "/api/clients/**", "clientsBuckets", RateLimitProperties.KeyStrategy.CLIENT_ID, Set.of("GET"), 1);
        clientRestricted.setClientIds(new LinkedHashSet<>(Set.of("other-client")));
        properties.setRules(List.of(clientRestricted));

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request("GET", "/api/clients/1", "10.0.0.1"), response, chain);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    private static RateLimitProperties.Rule rule(String id,
                                                 String pathPattern,
                                                 String cacheName,
                                                 RateLimitProperties.KeyStrategy strategy,
                                                 Set<String> methods,
                                                 long capacity) {
        RateLimitProperties.Rule rule = new RateLimitProperties.Rule();
        rule.setId(id);
        rule.setEnabled(true);
        rule.setOrder(0);
        rule.setPathPattern(pathPattern);
        rule.setCacheName(cacheName);
        rule.setKeyStrategy(strategy);
        rule.setMethods(new LinkedHashSet<>(methods));
        rule.setCapacity(capacity);
        rule.setWindowSeconds(3600);
        return rule;
    }

    private static MockHttpServletRequest request(String method, String path, String remoteAddr) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setServletPath(path);
        request.setRemoteAddr(remoteAddr);
        return request;
    }
}
