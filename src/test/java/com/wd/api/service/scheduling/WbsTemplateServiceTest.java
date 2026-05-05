package com.wd.api.service.scheduling;

import com.wd.api.model.enums.FloorLoop;
import com.wd.api.repository.scheduling.WbsTemplateRepository;
import com.wd.api.service.scheduling.dto.WbsTemplateDto;
import com.wd.api.service.scheduling.dto.WbsTemplatePhaseDto;
import com.wd.api.service.scheduling.dto.WbsTemplateTaskDto;
import com.wd.api.service.scheduling.dto.WbsTemplateTaskPredecessorDto;
import com.wd.api.testsupport.TestcontainersPostgresBase;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class WbsTemplateServiceTest extends TestcontainersPostgresBase {

    @Autowired
    private WbsTemplateService service;

    @Autowired
    private WbsTemplateRepository templates;

    private static WbsTemplateDto fixtureDto(String code, String name) {
        return new WbsTemplateDto(
                null, code, "RESIDENTIAL", name, "desc",
                null, null,
                List.of(new WbsTemplatePhaseDto(null, 1, "Substructure", "SITE_ENGINEER", false,
                        List.of(
                                new WbsTemplateTaskDto(-101L, 1, "Excavation", null,
                                        4, null, false, false, FloorLoop.NONE, null, List.of()),
                                new WbsTemplateTaskDto(-102L, 2, "Foundation", null,
                                        7, 9, false, true, FloorLoop.NONE, null,
                                        List.of(new WbsTemplateTaskPredecessorDto(null, -101L, 0, "FS")))
                        ))),
                null);
    }

    @Test
    void create_persistsVersion1AndIsActive() {
        WbsTemplateDto created = service.create(fixtureDto("RESIDENTIAL_T1", "Residential T1"));
        assertThat(created.id()).isNotNull();
        assertThat(created.version()).isEqualTo(1);
        assertThat(created.isActive()).isTrue();
        assertThat(created.code()).isEqualTo("RESIDENTIAL_T1");
        assertThat(created.phases()).hasSize(1);
        assertThat(created.phases().get(0).tasks()).hasSize(2);
        // Predecessor on task #2 → task #1
        assertThat(created.phases().get(0).tasks().get(1).predecessors()).hasSize(1);
    }

    @Test
    void list_returnsActiveOnly_whenIncludeInactiveFalse() {
        WbsTemplateDto v1 = service.create(fixtureDto("LIST_TEST", "v1"));
        // bump v2 active, v1 becomes inactive
        service.update(v1.id(), fixtureDto("LIST_TEST", "v2"));

        List<WbsTemplateDto> active = service.list(false);
        long matching = active.stream().filter(t -> "LIST_TEST".equals(t.code())).count();
        assertThat(matching).isEqualTo(1);

        List<WbsTemplateDto> all = service.list(true);
        long allMatching = all.stream().filter(t -> "LIST_TEST".equals(t.code())).count();
        assertThat(allMatching).isEqualTo(2);
    }

    @Test
    void getActiveByCode_returnsLatestActive() {
        WbsTemplateDto v1 = service.create(fixtureDto("ACTIVE_BY_CODE", "v1"));
        service.update(v1.id(), fixtureDto("ACTIVE_BY_CODE", "v2"));

        WbsTemplateDto active = service.getActiveByCode("ACTIVE_BY_CODE");
        assertThat(active.version()).isEqualTo(2);
        assertThat(active.name()).isEqualTo("v2");
    }

    @Test
    void update_createsNewVersionAndDeactivatesPrevious() {
        WbsTemplateDto v1 = service.create(fixtureDto("VER_TEST", "v1"));
        WbsTemplateDto v2 = service.update(v1.id(), fixtureDto("VER_TEST", "v2"));

        assertThat(v2.version()).isEqualTo(2);
        assertThat(v2.isActive()).isTrue();
        assertThat(templates.findById(v1.id()).orElseThrow().getIsActive()).isFalse();
    }

    @Test
    void deactivate_softDeletesAndKeepsRowQueryable() {
        WbsTemplateDto v1 = service.create(fixtureDto("DEACT_TEST", "v1"));
        service.deactivate(v1.id());

        WbsTemplateDto reloaded = service.get(v1.id());
        assertThat(reloaded.isActive()).isFalse();
        assertThatThrownBy(() -> service.getActiveByCode("DEACT_TEST"))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void getByCodeAndVersion_returnsHistorical() {
        WbsTemplateDto v1 = service.create(fixtureDto("HIST_TEST", "v1"));
        service.update(v1.id(), fixtureDto("HIST_TEST", "v2"));

        WbsTemplateDto historical = service.getByCodeAndVersion("HIST_TEST", 1);
        assertThat(historical.version()).isEqualTo(1);
        assertThat(historical.name()).isEqualTo("v1");
    }

    @Test
    void get_throwsForUnknownId() {
        assertThatThrownBy(() -> service.get(999_999L))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
