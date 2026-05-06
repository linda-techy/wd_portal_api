package com.wd.api.service.scheduling;

import com.wd.api.repository.CustomerProjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Boot-time backfill of CPM denormalized columns for every existing project.
 * Runs after schema migrations and HolidaySeeder; idempotent (recompute is
 * deterministic given fixed inputs, so re-runs produce identical column values).
 *
 * <p>Disabled in tests (the {@code !test} profile filter) so test contexts
 * don't pay the cost of recomputing every project on boot. Test classes that
 * need first-run behaviour seed projects + call {@link CpmService#recompute}
 * directly.
 */
@Component
@Profile("!test")
@Order(50) // after Flyway, after HolidaySeeder
public class CpmInitialPopulator implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(CpmInitialPopulator.class);

    private final CustomerProjectRepository projects;
    private final CpmService cpm;

    public CpmInitialPopulator(CustomerProjectRepository projects, CpmService cpm) {
        this.projects = projects;
        this.cpm = cpm;
    }

    @Override
    public void run(String... args) {
        long t0 = System.currentTimeMillis();
        int touched = 0;
        int failed = 0;
        for (var p : projects.findAll()) {
            try {
                cpm.recompute(p.getId());
                touched++;
            } catch (Exception ex) {
                failed++;
                log.warn("CpmInitialPopulator: project={} skipped due to {}", p.getId(), ex.toString());
            }
        }
        log.info("CpmInitialPopulator: touched={} failed={} totalDuration={}ms",
                touched, failed, System.currentTimeMillis() - t0);
    }
}
