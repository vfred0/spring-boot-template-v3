package com.template.config.api_key;

import com.template.api.http_errors.request_body.capture.RequestBodyCaptureFilter;
import com.template.config.security.JsonAccessDeniedHandler;
import com.template.config.security.JsonAuthEntryPoint;
import com.template.config.security.RateLimitingFilter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFilter;

@Configuration
@ConditionalOnProperty(name = "app.security.mode", havingValue = "API_KEY")
public class ApiKeySecurityConfig {

    @Bean
    @SuppressWarnings("java:S4502")
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   ApiKeyAuthenticationFilter apiKeyAuthenticationFilter,
                                                   JsonAuthEntryPoint jsonAuthEntryPoint,
                                                   JsonAccessDeniedHandler jsonAccessDeniedHandler,
                                                   RateLimitingFilter rateLimitingFilter) throws Exception {

        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .anyRequest().authenticated()
                )

                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(jsonAuthEntryPoint)
                        .accessDeniedHandler(jsonAccessDeniedHandler)
                );

        http.addFilterAfter(apiKeyAuthenticationFilter, AuthenticationFilter.class);
        http.addFilterAfter(new RequestBodyCaptureFilter(), ApiKeyAuthenticationFilter.class);
        http.addFilterAfter(rateLimitingFilter, ApiKeyAuthenticationFilter.class);

        return http.build();
    }
}
