package com.template.config.keycloak;

import com.template.api.http_errors.request_body.capture.RequestBodyCaptureFilter;
import com.template.config.security.JsonAccessDeniedHandler;
import com.template.config.security.JsonAuthEntryPoint;
import com.template.config.dpop.DpopAuthenticationFilter;
import com.template.config.dpop.DpopAwareBearerTokenResolver;
import com.template.config.security.RateLimitingFilter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFilter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;

@Configuration
@ConditionalOnProperty(name = "app.security.mode", havingValue = "KEYCLOAK_JWT")
public class KeycloakJwtSecurityConfig {

    @Bean
    public JwtDecoder jwtDecoder(KeycloakProperties props) {
        return NimbusJwtDecoder.withJwkSetUri(props.getJwkSetUri()).build();
    }

    @Bean
    public DpopAwareBearerTokenResolver dpopAwareBearerTokenResolver() {
        return new DpopAwareBearerTokenResolver();
    }

    @Bean
    @SuppressWarnings("java:S4502")
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtDecoder jwtDecoder,
                                                   KeycloakJwtAuthenticationConverter jwtAuthenticationConverter,
                                                   DpopAwareBearerTokenResolver dpopAwareBearerTokenResolver,
                                                   JsonAuthEntryPoint jsonAuthEntryPoint,
                                                   JsonAccessDeniedHandler jsonAccessDeniedHandler,
                                                   DpopAuthenticationFilter dpopAuthenticationFilter,
                                                   RateLimitingFilter rateLimitingFilter) throws Exception {

        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .anyRequest().authenticated()
                )

                .oauth2ResourceServer(oauth2 -> oauth2
                        .bearerTokenResolver(dpopAwareBearerTokenResolver)
                        .jwt(jwt -> jwt.decoder(jwtDecoder).jwtAuthenticationConverter(jwtAuthenticationConverter))
                        .authenticationEntryPoint(jsonAuthEntryPoint)
                        .accessDeniedHandler(jsonAccessDeniedHandler)
                )

                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(jsonAuthEntryPoint)
                        .accessDeniedHandler(jsonAccessDeniedHandler)
                );

        http.addFilterAfter(new RequestBodyCaptureFilter(), BearerTokenAuthenticationFilter.class);
        http.addFilterAfter(dpopAuthenticationFilter, AuthenticationFilter.class);
        http.addFilterAfter(rateLimitingFilter, DpopAuthenticationFilter.class);

        return http.build();
    }
}
