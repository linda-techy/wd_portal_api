package com.wd.api.estimation.persistence;

import com.wd.api.estimation.domain.Estimation;
import com.wd.api.estimation.domain.EstimationLineItem;
import com.wd.api.estimation.domain.enums.EstimationStatus;
import com.wd.api.estimation.domain.enums.LineType;
import com.wd.api.estimation.domain.enums.ProjectType;
import com.wd.api.model.Lead;
import com.wd.api.testsupport.TestcontainersPostgresBase;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class EstimationLineItemPersistenceTest extends TestcontainersPostgresBase {

    @Autowired
    private EntityManager em;

    @Test
    void roundTrip_estimationLineItem() {
        Lead lead = new Lead();
        lead.setName("Test");
        em.persist(lead);

        Estimation est = new Estimation();
        est.setEstimationNo("EST-2026-LINE");
        est.setLeadId(lead.getId());
        est.setProjectType(ProjectType.NEW_BUILD);
        est.setDimensionsJson(Map.<String, Object>of("floors", "[]"));
        est.setStatus(EstimationStatus.DRAFT);
        em.persist(est);
        em.flush();

        EstimationLineItem li = new EstimationLineItem();
        li.setEstimationId(est.getId());
        li.setLineType(LineType.BASE);
        li.setDescription("Base package cost (Standard, 1050 sqft)");
        li.setQuantity(new BigDecimal("1050.00"));
        li.setUnit("sqft");
        li.setUnitRate(new BigDecimal("2350.00"));
        li.setAmount(new BigDecimal("2467500.00"));
        li.setDisplayOrder(1);

        em.persist(li);
        em.flush();
        UUID id = li.getId();
        em.clear();

        EstimationLineItem loaded = em.find(EstimationLineItem.class, id);
        assertThat(loaded.getLineType()).isEqualTo(LineType.BASE);
        assertThat(loaded.getDescription()).startsWith("Base package");
        assertThat(loaded.getAmount()).isEqualByComparingTo("2467500.00");
        assertThat(loaded.getDisplayOrder()).isEqualTo(1);
    }
}
