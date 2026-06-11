package com.template.config.api_key;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.security.api-key")
public class ApiKeyProperties {

    private String header = "X-API-Key";
    private Duration lastUsedThrottle = Duration.ofMinutes(1);
}
