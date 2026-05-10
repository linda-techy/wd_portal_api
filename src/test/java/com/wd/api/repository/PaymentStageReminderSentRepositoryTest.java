package com.wd.api.repository;

import com.wd.api.model.BoqDocument;
import com.wd.api.model.CustomerProject;
import com.wd.api.model.PaymentStage;
import com.wd.api.model.PaymentStageReminderSent;
import com.wd.api.model.enums.PaymentStageStatus;
import com.wd.api.model.enums.ReminderKind;
import com.wd.api.testsupport.TestcontainersPostgresBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentStageReminderSentRepositoryTest extends TestcontainersPostgresBase {

    @Autowired PaymentStageReminderSentRepository repo;
    @Autowired PaymentStageRepository stageRepo;
    @Autowired com.wd.api.repository.CustomerProjectRepository projectRepo;
    @Autowired com.wd.api.repository.BoqDocumentRepository boqRepo;

    private PaymentStage stage;

    @BeforeEach
    void setUp() {
        repo.deleteAll();
        // Minimal fixture — a project, a BOQ, and one payment stage.
        // NB: customer_projects has NOT NULL on `location` and a UNIQUE on
        // `project_uuid`, so seed both. (Plan fixture omitted these — discovered
        // at test-run time, fixed in-task per the executor's stop-condition rules.)
        CustomerProject project = new CustomerProject();
        project.setName("S6-PR2 fixture project");
        project.setLocation("Bengaluru");
        project.setProjectUuid(java.util.UUID.randomUUID());
        project = projectRepo.save(project);

        BoqDocument boq = new BoqDocument();
        boq.setProject(project);
        boq = boqRepo.save(boq);

        stage = new PaymentStage();
        stage.setProject(project);
        stage.setBoqDocument(boq);
        stage.setStageNumber(1);
        stage.setStageName("Foundation");
        stage.setStatus(PaymentStageStatus.DUE);
        stage.setDueDate(LocalDate.of(2026, 5, 13));
        stage.setBoqValueSnapshot(new BigDecimal("3500000"));
        stage.setStagePercentage(new BigDecimal("0.1200"));
        stage.setStageAmountExGst(new BigDecimal("420000"));
        stage.setGstRate(new BigDecimal("0.1800"));
        stage.setGstAmount(new BigDecimal("75600"));
        stage.setStageAmountInclGst(new BigDecimal("495600"));
        stage = stageRepo.save(stage);
    }

    @Test
    void save_persistsAndReturnsId() {
        PaymentStageReminderSent row = new PaymentStageReminderSent();
        row.setStageId(stage.getId());
        row.setReminderKind(ReminderKind.T_MINUS_3);

        PaymentStageReminderSent saved = repo.save(row);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getSentAt()).isNotNull();
    }

    @Test
    void uniqueConstraint_blocksDuplicateStageAndKind() {
        PaymentStageReminderSent first = new PaymentStageReminderSent();
        first.setStageId(stage.getId());
        first.setReminderKind(ReminderKind.DUE_TODAY);
        repo.saveAndFlush(first);

        PaymentStageReminderSent second = new PaymentStageReminderSent();
        second.setStageId(stage.getId());
        second.setReminderKind(ReminderKind.DUE_TODAY);

        assertThatThrownBy(() -> repo.saveAndFlush(second))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void differentKindsForSameStage_areAllowed() {
        PaymentStageReminderSent a = new PaymentStageReminderSent();
        a.setStageId(stage.getId());
        a.setReminderKind(ReminderKind.T_MINUS_3);
        repo.saveAndFlush(a);

        PaymentStageReminderSent b = new PaymentStageReminderSent();
        b.setStageId(stage.getId());
        b.setReminderKind(ReminderKind.DUE_TODAY);
        repo.saveAndFlush(b);

        assertThat(repo.findAll()).hasSize(2);
    }

    @Test
    void insertIfAbsent_returnsTrueOnFirstInsertFalseOnSecond() {
        boolean firstInsert = repo.insertIfAbsent(stage.getId(), ReminderKind.OVERDUE);
        boolean secondInsert = repo.insertIfAbsent(stage.getId(), ReminderKind.OVERDUE);

        assertThat(firstInsert).isTrue();
        assertThat(secondInsert).isFalse();
        assertThat(repo.findAll()).hasSize(1);
    }
}
