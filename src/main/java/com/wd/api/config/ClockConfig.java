package com.wd.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * S6 PR2 — exposes a {@link Clock} bean fixed to Asia/Kolkata so jobs and
 * services that compute "today" (e.g. PaymentMilestoneReminderJob) can be
 * tested against a frozen instant without static {@code LocalDate.now()}
 * calls leaking into production code.
 *
 * Tests construct their own Clock.fixed(...) and inject it directly into the
 * collaborator under test (no @MockBean needed).
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock istClock() {
        return Clock.system(ZoneId.of("Asia/Kolkata"));
    }
}
