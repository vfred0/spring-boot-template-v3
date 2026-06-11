package com.template.config;

import com.template.config.keycloak.KeycloakProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class KeycloakPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ConfigurationPropertiesAutoConfiguration.class))
            .withUserConfiguration(TestConfig.class);

    @Test
    void bindsAllKeycloakProperties() {
        contextRunner.withPropertyValues(
                "keycloak.auth-server-url=http://auth",
                "keycloak.realm=test-realm",
                "keycloak.token-url=http://token",
                "keycloak.logout-url=http://logout",
                "keycloak.introspection-url=http://introspect",
                "keycloak.connect-timeout=3s",
                "keycloak.read-timeout=4s",
                "keycloak.client-id=client",
                "keycloak.client-secret=secret",
                "keycloak.resource-client-id=resource-client",
                "keycloak.resource-client-secret=resource-secret"
        ).run(context -> {
            assertThat(context).hasSingleBean(KeycloakProperties.class);
            KeycloakProperties props = context.getBean(KeycloakProperties.class);
            assertThat(props.getAuthServerUrl()).isEqualTo("http://auth");
            assertThat(props.getRealm()).isEqualTo("test-realm");
            assertThat(props.getTokenUrl()).isEqualTo("http://token");
            assertThat(props.getLogoutUrl()).isEqualTo("http://logout");
            assertThat(props.getIntrospectionUrl()).isEqualTo("http://introspect");
            assertThat(props.getConnectTimeout()).isEqualTo(Duration.ofSeconds(3));
            assertThat(props.getReadTimeout()).isEqualTo(Duration.ofSeconds(4));
            assertThat(props.getClientId()).isEqualTo("client");
            assertThat(props.getClientSecret()).isEqualTo("secret");
            assertThat(props.getResourceClientId()).isEqualTo("resource-client");
            assertThat(props.getResourceClientSecret()).isEqualTo("resource-secret");
        });
    }

    @Configuration
    @EnableConfigurationProperties(KeycloakProperties.class)
    static class TestConfig {
    }
}

