package com.wd.api.repository;

import com.wd.api.model.BoqDocument;
import com.wd.api.model.CustomerProject;
import com.wd.api.model.PaymentStage;
import com.wd.api.model.enums.PaymentStageStatus;
import com.wd.api.testsupport.TestcontainersPostgresBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentStageRepositoryReminderQueryTest extends TestcontainersPostgresBase {

    @Autowired PaymentStageRepository stageRepo;
    @Autowired CustomerProjectRepository projectRepo;
    @Autowired BoqDocumentRepository boqRepo;

    private CustomerProject project;
    private BoqDocument boq;
    private static final LocalDate TODAY = LocalDate.of(2026, 5, 10);

    @BeforeEach
    void setUp() {
        stageRepo.deleteAll();
        // customer_projects.location is NOT NULL; project_uuid is UNIQUE.
        project = new CustomerProject();
        project.setName("S6-PR2 reminder-query fixture");
        project.setLocation("Bengaluru");
        project.setProjectUuid(UUID.randomUUID());
        project = projectRepo.save(project);
        boq = new BoqDocument();
        boq.setProject(project);
        boq = boqRepo.save(boq);
    }

    /** Returns only stages whose status is NOT in (PAID, ON_HOLD) and whose due_date is non-null. */
    @Test
    void findCandidatesForReminder_filtersByStatusAndDueDate() {
        save("DUE +3", LocalDate.of(2026, 5, 13), PaymentStageStatus.DUE, 1);
        save("DUE today", TODAY, PaymentStageStatus.DUE, 2);
        save("OVERDUE", LocalDate.of(2026, 5, 8), PaymentStageStatus.OVERDUE, 3);
        save("PAID", LocalDate.of(2026, 5, 13), PaymentStageStatus.PAID, 4);
        save("ON_HOLD", LocalDate.of(2026, 5, 13), PaymentStageStatus.ON_HOLD, 5);
        save("INVOICED past", LocalDate.of(2026, 5, 8), PaymentStageStatus.INVOICED, 6);
        save("UPCOMING null due", null, PaymentStageStatus.UPCOMING, 7);

        List<PaymentStage> candidates = stageRepo.findCandidatesForReminder();

        // Stages 1, 2, 3, 6 qualify (status NOT in PAID/ON_HOLD AND due_date IS NOT NULL).
        // Stage 7 is excluded (due_date IS NULL). Stages 4 and 5 are excluded by status.
        assertThat(candidates).extracting(PaymentStage::getStageName)
                .containsExactlyInAnyOrder("DUE +3", "DUE today", "OVERDUE", "INVOICED past");
    }

    private void save(String name, LocalDate due, PaymentStageStatus status, int num) {
        PaymentStage s = new PaymentStage();
        s.setProject(project);
        s.setBoqDocument(boq);
        s.setStageName(name);
        s.setStageNumber(num);
        s.setDueDate(due);
        s.setStatus(status);
        s.setBoqValueSnapshot(new BigDecimal("3500000"));
        s.setStagePercentage(new BigDecimal("0.1200"));
        s.setStageAmountExGst(new BigDecimal("420000"));
        s.setGstRate(new BigDecimal("0.1800"));
        s.setGstAmount(new BigDecimal("75600"));
        s.setStageAmountInclGst(new BigDecimal("495600"));
        stageRepo.save(s);
    }
}
