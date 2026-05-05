package com.wd.api.service.scheduling;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.wd.api.model.scheduling.Holiday;
import com.wd.api.model.scheduling.HolidayOverrideAction;
import com.wd.api.model.scheduling.HolidayScope;
import com.wd.api.model.scheduling.ProjectHolidayOverride;
import com.wd.api.model.scheduling.ProjectScheduleConfig;
import com.wd.api.repository.HolidayRepository;
import com.wd.api.repository.ProjectHolidayOverrideRepository;
import com.wd.api.repository.ProjectScheduleConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Composes the effective set of non-working calendar dates for a project
 * over a date range:
 * <pre>
 * NATIONAL ∪ STATE(KL) ∪ DISTRICT(cfg.districtCode) ∪ project-ADD
 *   minus  project-EXCLUDE
 * </pre>
 *
 * Cached per (projectId, startYear, endYear) tuple via Caffeine.
 * Eviction is triggered explicitly by admin endpoints that mutate any
 * underlying table.
 */
@Service
public class HolidayService {

    private final HolidayRepository holidayRepo;
    private final ProjectHolidayOverrideRepository overrideRepo;
    private final ProjectScheduleConfigRepository configRepo;

    private final Cache<CacheKey, Set<LocalDate>> cache = Caffeine.newBuilder()
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .maximumSize(1024)
            .build();

    public HolidayService(HolidayRepository holidayRepo,
                          ProjectHolidayOverrideRepository overrideRepo,
                          ProjectScheduleConfigRepository configRepo) {
        this.holidayRepo = holidayRepo;
        this.overrideRepo = overrideRepo;
        this.configRepo = configRepo;
    }

    private record CacheKey(Long projectId, LocalDate start, LocalDate end) {}

    @Transactional(readOnly = true)
    public Set<LocalDate> holidaysFor(Long projectId, LocalDate start, LocalDate end) {
        return cache.get(new CacheKey(projectId, start, end), key -> compose(projectId, start, end));
    }

    private Set<LocalDate> compose(Long projectId, LocalDate start, LocalDate end) {
        Optional<ProjectScheduleConfig> cfg = configRepo.findByProjectId(projectId);
        String districtCode = cfg.map(ProjectScheduleConfig::getDistrictCode).orElse(null);

        Set<LocalDate> result = new HashSet<>();
        addAll(result, holidayRepo.findByScopeAndDateRange(HolidayScope.NATIONAL, null, start, end));
        addAll(result, holidayRepo.findByScopeAndDateRange(HolidayScope.STATE, "KL", start, end));
        if (districtCode != null) {
            addAll(result, holidayRepo.findByScopeAndDateRange(HolidayScope.DISTRICT, districtCode, start, end));
        }

        List<ProjectHolidayOverride> overrides = overrideRepo.findByProjectIdAndOverrideDateBetween(
                projectId, start, end);
        for (ProjectHolidayOverride o : overrides) {
            if (o.getAction() == HolidayOverrideAction.ADD) {
                result.add(o.getOverrideDate());
            } else if (o.getAction() == HolidayOverrideAction.EXCLUDE) {
                result.remove(o.getOverrideDate());
            }
        }
        return result;
    }

    private static void addAll(Set<LocalDate> sink, List<Holiday> source) {
        for (Holiday h : source) sink.add(h.getDate());
    }

    /** Invalidate every cached project. Call after any mutation. */
    public void evictAll() { cache.invalidateAll(); }

    /** Invalidate just the rows for one project. */
    public void evictProject(Long projectId) {
        cache.asMap().keySet().removeIf(k -> k.projectId().equals(projectId));
    }
}
