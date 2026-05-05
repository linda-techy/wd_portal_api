package com.wd.api.service.scheduling;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.wd.api.model.scheduling.Holiday;
import com.wd.api.model.scheduling.HolidayRecurrenceType;
import com.wd.api.model.scheduling.HolidayScope;
import com.wd.api.repository.HolidayRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;

/**
 * Boot-time idempotent loader for kerala-YYYY.yaml seed files.
 * Upserts on (code, date, scope, scopeRef).
 */
@Component
@Profile("!test")
public class HolidaySeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(HolidaySeeder.class);

    private final HolidayRepository repo;
    private final ObjectMapper yaml;

    public HolidaySeeder(HolidayRepository repo) {
        this.repo = repo;
        this.yaml = new ObjectMapper(new YAMLFactory());
        this.yaml.registerModule(new JavaTimeModule());
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] files = resolver.getResources("classpath:db/seed/holidays/*.yaml");
        int upserted = 0;
        for (Resource r : files) {
            try (InputStream in = r.getInputStream()) {
                HolidaySeedFile file = yaml.readValue(in, HolidaySeedFile.class);
                for (HolidaySeedFile.Entry e : file.holidays()) {
                    upserted += upsert(e) ? 1 : 0;
                }
            } catch (Exception ex) {
                log.error("Failed to load holiday seed {}: {}", r.getFilename(), ex.getMessage(), ex);
            }
        }
        log.info("HolidaySeeder: upserted {} rows from {} files", upserted, files.length);
    }

    private boolean upsert(HolidaySeedFile.Entry e) {
        HolidayScope scope = HolidayScope.valueOf(e.scope());
        HolidayRecurrenceType rec = HolidayRecurrenceType.valueOf(e.recurrence());
        return repo.findByDedupeKey(e.code(), e.date(), scope, e.scopeRef())
                .map(existing -> {
                    existing.setName(e.name());
                    existing.setRecurrenceType(rec);
                    existing.setSource("seed-" + e.date().getYear());
                    existing.setActive(true);
                    repo.save(existing);
                    return false;
                })
                .orElseGet(() -> {
                    Holiday h = new Holiday();
                    h.setCode(e.code());
                    h.setName(e.name());
                    h.setDate(e.date());
                    h.setScope(scope);
                    h.setScopeRef(e.scopeRef());
                    h.setRecurrenceType(rec);
                    h.setSource("seed-" + e.date().getYear());
                    h.setActive(true);
                    repo.save(h);
                    return true;
                });
    }
}
