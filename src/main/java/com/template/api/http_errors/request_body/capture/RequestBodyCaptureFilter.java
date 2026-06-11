package com.template.api.http_errors.request_body.capture;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

public class RequestBodyCaptureFilter extends OncePerRequestFilter {

    public static final String REQUEST_BODY_FILE_ATTRIBUTE = "REQUEST_BODY_FILE";
    private static final Set<String> CAPTURED_METHODS = Set.of("POST", "PATCH", "PUT");
    private static final String TEMP_FILE_PREFIX = "payload_";
    private static final String TEMP_FILE_SUFFIX = ".json";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!CAPTURED_METHODS.contains(request.getMethod().toUpperCase())) {
            filterChain.doFilter(request, response);
            return;
        }
        var tempFile = captureBody(request);
        request.setAttribute(REQUEST_BODY_FILE_ATTRIBUTE, tempFile.toString());
        try {
            filterChain.doFilter(wrapRequest(request, tempFile), response);
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    private Path captureBody(HttpServletRequest request) throws IOException {
        var tempFile = Files.createTempFile(TEMP_FILE_PREFIX, TEMP_FILE_SUFFIX);
        try (var in = request.getInputStream(); var out = Files.newOutputStream(tempFile)) {
            in.transferTo(out);
        }
        return tempFile;
    }

    private HttpServletRequestWrapper wrapRequest(HttpServletRequest request, Path tempFile) {
        return new HttpServletRequestWrapper(request) {
            @Override
            public jakarta.servlet.ServletInputStream getInputStream() throws IOException {
                return new ReplayableServletInputStream(Files.newInputStream(tempFile));
            }
        };
    }
}
