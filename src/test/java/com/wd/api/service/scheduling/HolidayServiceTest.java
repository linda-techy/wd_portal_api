package com.wd.api.service.scheduling;

import com.wd.api.model.scheduling.Holiday;
import com.wd.api.model.scheduling.HolidayOverrideAction;
import com.wd.api.model.scheduling.HolidayRecurrenceType;
import com.wd.api.model.scheduling.HolidayScope;
import com.wd.api.model.scheduling.ProjectHolidayOverride;
import com.wd.api.model.scheduling.ProjectScheduleConfig;
import com.wd.api.repository.HolidayRepository;
import com.wd.api.repository.ProjectHolidayOverrideRepository;
import com.wd.api.repository.ProjectScheduleConfigRepository;
import com.wd.api.testsupport.TestcontainersPostgresBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class HolidayServiceTest extends TestcontainersPostgresBase {

    @Autowired private HolidayService service;
    @Autowired private HolidayRepository holidayRepo;
    @Autowired private ProjectHolidayOverrideRepository overrideRepo;
    @Autowired private ProjectScheduleConfigRepository configRepo;

    private static final Long PROJECT_ID = 7777L;

    private Holiday holiday(String code, LocalDate date, HolidayScope scope, String scopeRef) {
        Holiday h = new Holiday();
        h.setCode(code);
        h.setName(code);
        h.setDate(date);
        h.setScope(scope);
        h.setScopeRef(scopeRef);
        h.setRecurrenceType(HolidayRecurrenceType.FIXED_DATE);
        h.setActive(true);
        return holidayRepo.save(h);
    }

    @BeforeEach
    void clean() {
        overrideRepo.deleteAll();
        configRepo.deleteAll();
        holidayRepo.deleteAll();
        service.evictAll();
    }

    @AfterEach
    void evict() { service.evictAll(); }

    @Test
    void composes_NATIONAL_STATE_DISTRICT_minus_EXCLUDE_plus_ADD() {
        // setup: project schedule config with district KL-EKM
        ProjectScheduleConfig cfg = new ProjectScheduleConfig();
        cfg.setProjectId(PROJECT_ID);
        cfg.setDistrictCode("KL-EKM");
        configRepo.save(cfg);

        Holiday national = holiday("IN_X", LocalDate.of(2026, 8, 15), HolidayScope.NATIONAL, null);
        Holiday state = holiday("KL_X", LocalDate.of(2026, 8, 27), HolidayScope.STATE, "KL");
        Holiday district = holiday("KL_EKM_X", LocalDate.of(2026, 9, 10), HolidayScope.DISTRICT, "KL-EKM");

        // EXCLUDE the state holiday for this project
        ProjectHolidayOverride exclude = new ProjectHolidayOverride();
        exclude.setProjectId(PROJECT_ID);
        exclude.setHolidayId(state.getId());
        exclude.setOverrideDate(state.getDate());
        exclude.setAction(HolidayOverrideAction.EXCLUDE);
        overrideRepo.save(exclude);

        // ADD a project-only day
        ProjectHolidayOverride add = new ProjectHolidayOverride();
        add.setProjectId(PROJECT_ID);
        add.setOverrideDate(LocalDate.of(2026, 12, 31));
        add.setOverrideName("Office shutdown");
        add.setAction(HolidayOverrideAction.ADD);
        overrideRepo.save(add);

        Set<LocalDate> result = service.holidaysFor(PROJECT_ID,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));

        assertThat(result).containsExactlyInAnyOrder(
                LocalDate.of(2026, 8, 15),   // national: kept
                LocalDate.of(2026, 9, 10),   // district: kept
                LocalDate.of(2026, 12, 31)); // ADD
        assertThat(result).doesNotContain(LocalDate.of(2026, 8, 27)); // EXCLUDED
    }

    @Test
    void noConfig_meansNoDistrict_butStateAndNationalStillApply() {
        holiday("IN_REPUBLIC", LocalDate.of(2026, 1, 26), HolidayScope.NATIONAL, null);
        holiday("KL_VISHU", LocalDate.of(2026, 4, 14), HolidayScope.STATE, "KL");
        holiday("KL_EKM_X", LocalDate.of(2026, 5, 1), HolidayScope.DISTRICT, "KL-EKM");

        Set<LocalDate> result = service.holidaysFor(PROJECT_ID,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));

        assertThat(result).contains(LocalDate.of(2026, 1, 26), LocalDate.of(2026, 4, 14));
        assertThat(result).doesNotContain(LocalDate.of(2026, 5, 1));
    }

    @Test
    void cacheEviction_returnsFreshAfterEdit() {
        ProjectScheduleConfig cfg = new ProjectScheduleConfig();
        cfg.setProjectId(PROJECT_ID);
        configRepo.save(cfg);

        holiday("IN_A", LocalDate.of(2026, 6, 1), HolidayScope.NATIONAL, null);
        Set<LocalDate> first = service.holidaysFor(PROJECT_ID,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
        assertThat(first).containsExactly(LocalDate.of(2026, 6, 1));

        holiday("IN_B", LocalDate.of(2026, 7, 4), HolidayScope.NATIONAL, null);
        service.evictAll();

        Set<LocalDate> second = service.holidaysFor(PROJECT_ID,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
        assertThat(second).containsExactlyInAnyOrder(
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 7, 4));
    }
}
