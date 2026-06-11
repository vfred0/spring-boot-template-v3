package com.template.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitPropertiesTest {

    @Test
    void headerValidationFailsWhenHeaderStrategyWithoutHeaderName() {
        RateLimitProperties.Rule rule = new RateLimitProperties.Rule();
        rule.setKeyStrategy(RateLimitProperties.KeyStrategy.HEADER);
        rule.setKeyHeader(" ");

        assertThat(rule.isHeaderValid()).isFalse();
    }

    @Test
    void headerValidationPassesWhenHeaderStrategyWithHeaderName() {
        RateLimitProperties.Rule rule = new RateLimitProperties.Rule();
        rule.setKeyStrategy(RateLimitProperties.KeyStrategy.HEADER);
        rule.setKeyHeader("X-Request-Id");

        assertThat(rule.isHeaderValid()).isTrue();
    }

    @Test
    void headerValidationPassesForNonHeaderStrategy() {
        RateLimitProperties.Rule rule = new RateLimitProperties.Rule();
        rule.setKeyStrategy(RateLimitProperties.KeyStrategy.IP);
        rule.setKeyHeader(" ");

        assertThat(rule.isHeaderValid()).isTrue();
    }
}

