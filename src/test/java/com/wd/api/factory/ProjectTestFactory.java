package com.wd.api.factory;

import com.wd.api.model.CustomerProject;
import com.wd.api.model.enums.ContractType;
import com.wd.api.model.enums.ProjectPhase;
import com.wd.api.model.enums.ProjectStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Static factory methods for creating {@link CustomerProject} test fixtures.
 * <p>
 * Returned entities are NOT persisted -- the calling test is responsible for
 * saving them via a repository (which also allows setting the customer,
 * project manager, and other relationships before persistence).
 */
public final class ProjectTestFactory {

    private ProjectTestFactory() {}

    /**
     * A typical residential villa project in the PLANNING phase.
     */
    public static CustomerProject residential() {
        CustomerProject p = new CustomerProject();
        p.setProjectUuid(UUID.randomUUID());
        p.setName("Residential Villa Test");
        p.setLocation("Test Location");
        p.setProjectType("RESIDENTIAL");
        p.setBudget(new BigDecimal("5000000"));
        p.setSqfeet(new BigDecimal("2500"));
        p.setProjectPhase(ProjectPhase.PLANNING);
        p.setProjectStatus(ProjectStatus.ACTIVE);
        p.setContractType(ContractType.TURNKEY);
        p.setStartDate(LocalDate.now());
        p.setEndDate(LocalDate.now().plusMonths(12));
        p.setState("Karnataka");
        p.setDistrict("Bangalore Urban");
        return p;
    }

    /**
     * A commercial office project in the PLANNING phase.
     */
    public static CustomerProject commercial() {
        CustomerProject p = new CustomerProject();
        p.setProjectUuid(UUID.randomUUID());
        p.setName("Commercial Office Test");
        p.setLocation("Test Location");
        p.setProjectType("COMMERCIAL");
        p.setBudget(new BigDecimal("50000000"));
        p.setSqfeet(new BigDecimal("25000"));
        p.setProjectPhase(ProjectPhase.PLANNING);
        p.setProjectStatus(ProjectStatus.ACTIVE);
        p.setContractType(ContractType.TURNKEY);
        p.setStartDate(LocalDate.now());
        p.setEndDate(LocalDate.now().plusMonths(24));
        p.setState("Karnataka");
        p.setDistrict("Bangalore Urban");
        return p;
    }

    /**
     * A renovation project already in the CONSTRUCTION phase.
     */
    public static CustomerProject renovation() {
        CustomerProject p = new CustomerProject();
        p.setProjectUuid(UUID.randomUUID());
        p.setName("Renovation Project Test");
        p.setLocation("Test Location");
        p.setProjectType("RENOVATION");
        p.setBudget(new BigDecimal("2000000"));
        p.setSqfeet(new BigDecimal("1500"));
        p.setProjectPhase(ProjectPhase.CONSTRUCTION);
        p.setProjectStatus(ProjectStatus.ACTIVE);
        p.setContractType(ContractType.TURNKEY);
        p.setStartDate(LocalDate.now().minusMonths(3));
        p.setEndDate(LocalDate.now().plusMonths(6));
        p.setState("Karnataka");
        p.setDistrict("Bangalore Urban");
        return p;
    }
}
