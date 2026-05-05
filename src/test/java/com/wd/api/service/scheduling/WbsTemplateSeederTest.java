package com.wd.api.service.scheduling;

import com.wd.api.model.enums.FloorLoop;
import com.wd.api.model.scheduling.WbsTemplate;
import com.wd.api.repository.scheduling.WbsTemplatePhaseRepository;
import com.wd.api.repository.scheduling.WbsTemplateRepository;
import com.wd.api.repository.scheduling.WbsTemplateTaskPredecessorRepository;
import com.wd.api.repository.scheduling.WbsTemplateTaskRepository;
import com.wd.api.testsupport.TestcontainersPostgresBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Boots the WbsTemplateSeeder against the four classpath YAMLs and verifies:
 * idempotent re-runs, content-hash-driven version bump, and faithful
 * persistence of phase / task / predecessor structure.
 */
@Transactional
class WbsTemplateSeederTest extends TestcontainersPostgresBase {

    @Autowired
    private WbsTemplateSeeder seeder;

    @Autowired
    private WbsTemplateRepository templates;

    @Autowired
    private WbsTemplatePhaseRepository phases;

    @Autowired
    private WbsTemplateTaskRepository tasks;

    @Autowired
    private WbsTemplateTaskPredecessorRepository preds;

    @Test
    void runOnce_seedsFourTemplates() throws Exception {
        seeder.seedFromClasspath();

        for (String code : List.of("RESIDENTIAL", "COMMERCIAL", "INTERIOR_FITOUT", "RENOVATION")) {
            Optional<WbsTemplate> active = templates.findByCodeAndIsActiveTrue(code);
            assertThat(active).as("active template for %s", code).isPresent();
            assertThat(active.get().getVersion()).isEqualTo(1);
            assertThat(active.get().getSourceHash()).as("hash for %s", code).isNotBlank();
        }
    }

    @Test
    void runTwice_isIdempotent() throws Exception {
        seeder.seedFromClasspath();
        long count1 = templates.findAll().stream()
                .filter(t -> List.of("RESIDENTIAL", "COMMERCIAL",
                                     "INTERIOR_FITOUT", "RENOVATION").contains(t.getCode()))
                .count();

        seeder.seedFromClasspath();
        long count2 = templates.findAll().stream()
                .filter(t -> List.of("RESIDENTIAL", "COMMERCIAL",
                                     "INTERIOR_FITOUT", "RENOVATION").contains(t.getCode()))
                .count();

        assertThat(count2).as("idempotent re-run must not bump versions").isEqualTo(count1);
        // All four still on v1
        for (String code : List.of("RESIDENTIAL", "COMMERCIAL", "INTERIOR_FITOUT", "RENOVATION")) {
            WbsTemplate active = templates.findByCodeAndIsActiveTrue(code).orElseThrow();
            assertThat(active.getVersion()).isEqualTo(1);
        }
    }

    @Test
    void contentChange_bumpsVersion() throws Exception {
        seeder.seedFromClasspath();
        WbsTemplate residentialV1 = templates.findByCodeAndIsActiveTrue("RESIDENTIAL").orElseThrow();
        String originalHash = residentialV1.getSourceHash();

        // Tamper with the stored hash so the seeder thinks the YAML changed.
        residentialV1.setSourceHash("tampered-not-matching-real-hash");
        templates.save(residentialV1);
        templates.flush();

        seeder.seedFromClasspath();

        WbsTemplate active = templates.findByCodeAndIsActiveTrue("RESIDENTIAL").orElseThrow();
        assertThat(active.getVersion()).isEqualTo(2);
        assertThat(active.getSourceHash()).isEqualTo(originalHash);

        WbsTemplate previousV1 = templates.findByCodeAndVersion("RESIDENTIAL", 1).orElseThrow();
        assertThat(previousV1.getIsActive()).isFalse();
    }

    @Test
    void phasesAndTasksMatchYaml_residential() throws Exception {
        seeder.seedFromClasspath();

        WbsTemplate residential = templates.findByCodeAndIsActiveTrue("RESIDENTIAL").orElseThrow();
        var phaseRows = phases.findByTemplateIdOrderBySequenceAsc(residential.getId());
        assertThat(phaseRows).hasSize(5);
        assertThat(phaseRows.get(0).getName()).isEqualTo("Substructure");
        assertThat(phaseRows.get(1).getName()).isEqualTo("Superstructure");
        assertThat(phaseRows.get(2).getName()).startsWith("MEP");
        assertThat(phaseRows.get(3).getName()).isEqualTo("Finishing");
        assertThat(phaseRows.get(4).getName()).isEqualTo("Handover");

        // Slab Casting in Superstructure must be PER_FLOOR + monsoon-sensitive.
        var superstructureTasks = tasks.findByPhaseIdOrderBySequenceAsc(phaseRows.get(1).getId());
        boolean foundPerFloorMonsoon = superstructureTasks.stream()
                .anyMatch(t -> "Slab Casting".equals(t.getName())
                        && t.getFloorLoop() == FloorLoop.PER_FLOOR
                        && Boolean.TRUE.equals(t.getMonsoonSensitive()));
        assertThat(foundPerFloorMonsoon)
                .as("Slab Casting must be PER_FLOOR + monsoon-sensitive")
                .isTrue();
    }

    @Test
    void predecessorsArePersisted() throws Exception {
        seeder.seedFromClasspath();

        WbsTemplate residential = templates.findByCodeAndIsActiveTrue("RESIDENTIAL").orElseThrow();
        var allEdges = preds.findAllForTemplate(residential.getId());
        assertThat(allEdges)
                .as("residential template must have predecessor edges")
                .isNotEmpty();
    }
}
