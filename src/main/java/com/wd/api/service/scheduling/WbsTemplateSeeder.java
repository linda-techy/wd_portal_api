package com.wd.api.service.scheduling;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wd.api.model.enums.FloorLoop;
import com.wd.api.model.scheduling.WbsTemplate;
import com.wd.api.model.scheduling.WbsTemplatePhase;
import com.wd.api.model.scheduling.WbsTemplateTask;
import com.wd.api.model.scheduling.WbsTemplateTaskPredecessor;
import com.wd.api.repository.scheduling.WbsTemplatePhaseRepository;
import com.wd.api.repository.scheduling.WbsTemplateRepository;
import com.wd.api.repository.scheduling.WbsTemplateTaskPredecessorRepository;
import com.wd.api.repository.scheduling.WbsTemplateTaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Boot-time seeder that loads {@code db/seed/wbs/*.yaml} and upserts each
 * template by content hash:
 * <ol>
 *   <li>If no row with the YAML's {@code code} exists → insert v1.</li>
 *   <li>If a row exists with matching {@code source_hash} → no-op.</li>
 *   <li>If a row exists with a different hash → mark previous inactive,
 *       insert a new row with version = max+1, isActive=true.</li>
 * </ol>
 *
 * <p>Snapshot semantics are preserved: existing project clones don't
 * reference these rows, so a version bump never breaks history.
 */
@Component
public class WbsTemplateSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(WbsTemplateSeeder.class);
    private static final String SEED_PATTERN = "classpath:db/seed/wbs/*.yaml";

    private final ObjectMapper yaml;
    private final WbsTemplateRepository templates;
    private final WbsTemplatePhaseRepository phases;
    private final WbsTemplateTaskRepository tasks;
    private final WbsTemplateTaskPredecessorRepository preds;

    public WbsTemplateSeeder(@Qualifier("scheduleYamlMapper") ObjectMapper yaml,
                             WbsTemplateRepository templates,
                             WbsTemplatePhaseRepository phases,
                             WbsTemplateTaskRepository tasks,
                             WbsTemplateTaskPredecessorRepository preds) {
        this.yaml = yaml;
        this.templates = templates;
        this.phases = phases;
        this.tasks = tasks;
        this.preds = preds;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        seedFromClasspath();
    }

    /** Public entry point so tests can drive the seeder without a CommandLineRunner harness. */
    @Transactional
    public void seedFromClasspath() throws Exception {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] yamls = resolver.getResources(SEED_PATTERN);
        for (Resource r : yamls) {
            try (var in = r.getInputStream()) {
                byte[] bytes = in.readAllBytes();
                String hash = sha256(bytes);
                YamlTemplate parsed = yaml.readValue(bytes, YamlTemplate.class);
                upsertTemplate(parsed, hash);
            } catch (Exception e) {
                log.error("Failed to seed wbs template from {}: {}",
                          r.getFilename(), e.toString(), e);
                throw e;
            }
        }
    }

    private void upsertTemplate(YamlTemplate parsed, String newHash) {
        Optional<WbsTemplate> activeOpt = templates.findByCodeAndIsActiveTrue(parsed.code);
        if (activeOpt.isPresent() && newHash.equals(activeOpt.get().getSourceHash())) {
            log.debug("WBS template {} unchanged (hash match) — skipping", parsed.code);
            return;
        }
        int nextVersion = templates.findMaxVersionForCode(parsed.code).orElse(0) + 1;
        activeOpt.ifPresent(prev -> {
            prev.setIsActive(Boolean.FALSE);
            templates.save(prev);
        });
        templates.flush();

        WbsTemplate t = new WbsTemplate();
        t.setCode(parsed.code);
        t.setProjectType(parsed.projectType);
        t.setName(parsed.name);
        t.setDescription(parsed.description);
        t.setVersion(nextVersion);
        t.setIsActive(Boolean.TRUE);
        t.setSourceHash(newHash);
        t.setCreatedBy("system-seeder");
        t.setUpdatedBy("system-seeder");
        t = templates.save(t);

        // First pass: persist all phases + tasks; build dto-id → entity map.
        Map<Long, WbsTemplateTask> byDtoId = new HashMap<>();
        if (parsed.phases != null) {
            for (YamlPhase yp : parsed.phases) {
                WbsTemplatePhase phase = new WbsTemplatePhase();
                phase.setTemplate(t);
                phase.setSequence(yp.sequence);
                phase.setName(yp.name);
                phase.setRoleHint(yp.roleHint);
                phase.setMonsoonSensitive(Boolean.TRUE.equals(yp.monsoonSensitive));
                phase = phases.save(phase);
                if (yp.tasks != null) {
                    for (YamlTask yt : yp.tasks) {
                        WbsTemplateTask task = new WbsTemplateTask();
                        task.setPhase(phase);
                        task.setSequence(yt.sequence);
                        task.setName(yt.name);
                        task.setRoleHint(yt.roleHint);
                        task.setDurationDays(yt.durationDays);
                        task.setWeightFactor(yt.weightFactor);
                        task.setMonsoonSensitive(Boolean.TRUE.equals(yt.monsoonSensitive));
                        task.setIsPaymentMilestone(Boolean.TRUE.equals(yt.isPaymentMilestone));
                        task.setFloorLoop(yt.floorLoop != null ? yt.floorLoop : FloorLoop.NONE);
                        task.setOptionalCost(yt.optionalCost);
                        task = tasks.save(task);
                        if (yt.id != null) byDtoId.put(yt.id, task);
                    }
                }
            }
        }
        // Second pass: predecessors.
        if (parsed.phases != null) {
            for (YamlPhase yp : parsed.phases) {
                if (yp.tasks == null) continue;
                for (YamlTask yt : yp.tasks) {
                    if (yt.predecessors == null || yt.id == null) continue;
                    WbsTemplateTask successor = byDtoId.get(yt.id);
                    if (successor == null) continue;
                    for (YamlPredecessor yp2 : yt.predecessors) {
                        WbsTemplateTask pred = byDtoId.get(yp2.predecessorTaskId);
                        if (pred == null) {
                            log.warn("Predecessor {} not found for task '{}' in template {}",
                                     yp2.predecessorTaskId, yt.name, parsed.code);
                            continue;
                        }
                        WbsTemplateTaskPredecessor entity = new WbsTemplateTaskPredecessor();
                        entity.setSuccessor(successor);
                        entity.setPredecessor(pred);
                        entity.setLagDays(yp2.lagDays != null ? yp2.lagDays : 0);
                        entity.setDepType(yp2.depType != null ? yp2.depType : "FS");
                        preds.save(entity);
                    }
                }
            }
        }
        log.info("Seeded WBS template {} v{} from classpath", parsed.code, nextVersion);
    }

    private static String sha256(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] dig = md.digest(bytes);
            StringBuilder sb = new StringBuilder(64);
            for (byte b : dig) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    // ---- YAML schema (Jackson-bound) ----
    public static class YamlTemplate {
        public String code;
        public String projectType;
        public String name;
        public String description;
        public List<YamlPhase> phases;
    }

    public static class YamlPhase {
        public Integer sequence;
        public String name;
        public String roleHint;
        public Boolean monsoonSensitive;
        public List<YamlTask> tasks;
    }

    public static class YamlTask {
        public Long id;
        public Integer sequence;
        public String name;
        public String roleHint;
        public Integer durationDays;
        public Integer weightFactor;
        public Boolean monsoonSensitive;
        public Boolean isPaymentMilestone;
        public FloorLoop floorLoop;
        public BigDecimal optionalCost;
        public List<YamlPredecessor> predecessors;
    }

    public static class YamlPredecessor {
        public Long predecessorTaskId;
        public Integer lagDays;
        public String depType;
    }
}
