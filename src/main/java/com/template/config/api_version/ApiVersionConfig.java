package com.template.config.api_version;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ApiVersionConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ApiVersionConfig implements WebMvcConfigurer {

    @Override
    public void configureApiVersioning(ApiVersionConfigurer configurer) {
        configurer
                .useRequestHeader("X-API-VERSION")
                .addSupportedVersions(ApiVersion.V1, ApiVersion.V2)
                .setDefaultVersion(ApiVersion.V1);
    }
}
