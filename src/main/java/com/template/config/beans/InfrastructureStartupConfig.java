package com.template.config.beans;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class InfrastructureStartupConfig {

    private final Flyway flyway;
    private final Scheduler scheduler;

    @Bean
    public ApplicationRunner infrastructureStartupRunner() {
        return args -> {
            flyway.migrate();
            if (!scheduler.isStarted()) {
                log.info("Starting Quartz scheduler after Flyway migrations");
                scheduler.start();
            }
        };
    }
}

