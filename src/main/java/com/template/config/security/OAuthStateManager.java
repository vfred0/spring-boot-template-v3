package com.template.config.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.UUID;

@Component
public class OAuthStateManager {

    private static final String STATE_COOKIE = "oauth2_state";
    private static final String COOKIE_PATH = "/api/auth";
    private static final int STATE_TTL_SECONDS = 600;

    public String generateAndWrite(HttpServletResponse response) {
        String state = UUID.randomUUID().toString();
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie(state, STATE_TTL_SECONDS).toString());
        return state;
    }

    public String extract(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        return Arrays.stream(cookies)
                .filter(c -> STATE_COOKIE.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    public void clear(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie("", 0).toString());
    }

    private ResponseCookie buildCookie(String value, int maxAge) {
        return ResponseCookie.from(STATE_COOKIE, value)
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .path(COOKIE_PATH)
                .maxAge(maxAge)
                .build();
    }
}
