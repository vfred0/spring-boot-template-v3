package com.template.api.http_errors.request_body.capture;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.nio.file.Files;
import java.nio.file.Paths;

@Slf4j
@Component
public class RequestBodyReader {

    public String read() {
        try {
            var attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) return null;
            var path = (String) attributes.getRequest().getAttribute(RequestBodyCaptureFilter.REQUEST_BODY_FILE_ATTRIBUTE);
            return path != null ? Files.readString(Paths.get(path)) : null;
        } catch (Exception e) {
            log.debug("Failed to retrieve request body", e);
            return null;
        }
    }
}
