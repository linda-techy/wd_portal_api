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
        // NB: we don't deleteAll() here — other integration tests may have left
        // payment_stages referenced by boq_invoices, and the FK from boq_invoices
        // (no CASCADE) makes a global cleanup intractable. Instead we tag every
        // stage with a unique stage-name prefix and assert containment, not
        // exact-list equality.
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
        // Unique tag so we can isolate this test's rows from any pre-existing
        // payment_stages in the shared Postgres container.
        String tag = "S6PR2-" + UUID.randomUUID().toString().substring(0, 8) + "-";
        save(tag + "DUE+3", LocalDate.of(2026, 5, 13), PaymentStageStatus.DUE, 1);
        save(tag + "DUEtoday", TODAY, PaymentStageStatus.DUE, 2);
        save(tag + "OVERDUE", LocalDate.of(2026, 5, 8), PaymentStageStatus.OVERDUE, 3);
        save(tag + "PAID", LocalDate.of(2026, 5, 13), PaymentStageStatus.PAID, 4);
        save(tag + "ONHOLD", LocalDate.of(2026, 5, 13), PaymentStageStatus.ON_HOLD, 5);
        save(tag + "INVOICEDpast", LocalDate.of(2026, 5, 8), PaymentStageStatus.INVOICED, 6);
        save(tag + "UPCOMINGnull", null, PaymentStageStatus.UPCOMING, 7);

        List<PaymentStage> candidates = stageRepo.findCandidatesForReminder().stream()
                .filter(s -> s.getStageName() != null && s.getStageName().startsWith(tag))
                .toList();

        // Stages 1, 2, 3, 6 qualify (status NOT in PAID/ON_HOLD AND due_date IS NOT NULL).
        // Stage 7 is excluded (due_date IS NULL). Stages 4 and 5 are excluded by status.
        assertThat(candidates).extracting(PaymentStage::getStageName)
                .containsExactlyInAnyOrder(
                        tag + "DUE+3", tag + "DUEtoday",
                        tag + "OVERDUE", tag + "INVOICEDpast");
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
