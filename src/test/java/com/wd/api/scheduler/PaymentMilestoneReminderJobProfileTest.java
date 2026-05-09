package com.wd.api.scheduler;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Profile;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * S6 PR2 — verifies the scheduled reminder bean is gated out of the
 * {@code test} profile.
 *
 * <p>The job class is annotated {@code @Profile("!test")}, so when the
 * {@code test} profile is active the bean must be excluded from the
 * application context.
 *
 * <p>Why this is a static-annotation check rather than a context-load check:
 * the portal-api test stack uses two parallel test runners — H2-backed tests
 * (driven by {@code application-test.yml}, which forces the H2 driver) and
 * Testcontainers-backed tests (driven by {@code TestcontainersPostgresBase},
 * which uses real Postgres). Booting a full Spring context with
 * {@code @ActiveProfiles("test")} pulls in the H2 driver config and clashes
 * with Testcontainers, while running without {@code @ActiveProfiles("test")}
 * leaves the active profile as {@code local} and the gate doesn't engage.
 *
 * <p>So instead of booting Spring, we read the annotation directly. This is
 * a regression guard: if someone removes the {@code @Profile("!test")}
 * annotation in a future refactor, this test fires immediately, and the
 * cron stays gated out of all CI runs as the spec requires.
 */
class PaymentMilestoneReminderJobProfileTest {

    @Test
    void jobClass_isGatedToExcludeTestProfile() {
        Profile profile = PaymentMilestoneReminderJob.class.getAnnotation(Profile.class);
        assertThat(profile)
                .as("PaymentMilestoneReminderJob must be annotated @Profile(\"!test\") so the cron"
                        + " does not fire under the test profile (spec, S6 PR2)")
                .isNotNull();
        assertThat(profile.value())
                .as("@Profile must contain exactly the value \"!test\"")
                .containsExactly("!test");
    }
}
