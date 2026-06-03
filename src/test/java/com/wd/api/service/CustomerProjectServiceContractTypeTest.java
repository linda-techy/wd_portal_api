package com.wd.api.service;

import com.wd.api.dto.CustomerProjectCreateRequest;
import com.wd.api.model.CustomerProject;
import com.wd.api.model.enums.ContractType;
import com.wd.api.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

/**
 * TDD slice for the contract-type guard on new-project creation.
 *
 * <p>Only TURNKEY and ITEM_RATE are offered for new projects.
 * LABOR_ONLY and COST_PLUS are rejected at creation time with a clear
 * IllegalArgumentException, but kept in the enum for backward-compat so
 * existing project rows still load without error.
 */
@ExtendWith(MockitoExtension.class)
class CustomerProjectServiceContractTypeTest {

    @Mock private CustomerProjectRepository customerProjectRepository;
    @Mock private CustomerUserRepository customerUserRepository;
    @Mock private ProjectMemberRepository projectMemberRepository;
    @Mock private PortalUserRepository portalUserRepository;
    @Mock private LeadRepository leadRepository;
    @Mock private ActivityFeedRepository activityFeedRepository;
    @Mock private QualityCheckRepository qualityCheckRepository;
    @Mock private PaymentScheduleRepository paymentScheduleRepository;
    @Mock private CustomerNotificationFacade customerNotificationFacade;

    private CustomerProjectService service;

    @BeforeEach
    void setUp() {
        service = new CustomerProjectService(
                customerProjectRepository,
                customerUserRepository,
                projectMemberRepository,
                portalUserRepository,
                leadRepository,
                activityFeedRepository,
                qualityCheckRepository,
                paymentScheduleRepository,
                customerNotificationFacade);

        // Allow save to echo back the entity for the happy-path tests
        lenient().when(customerProjectRepository.save(any(CustomerProject.class)))
                .thenAnswer(inv -> {
                    CustomerProject p = inv.getArgument(0);
                    p.setId(1L);
                    return p;
                });
    }

    // ── happy-path ─────────────────────────────────────────────────────────────

    @Test
    void createProject_withTurnkey_succeeds() {
        CustomerProject saved = service.createProject(minimalRequest("TURNKEY"), "test@wd.com");
        assertThat(saved.getContractType()).isEqualTo(ContractType.TURNKEY);
    }

    @Test
    void createProject_withItemRate_succeeds() {
        CustomerProject saved = service.createProject(minimalRequest("ITEM_RATE"), "test@wd.com");
        assertThat(saved.getContractType()).isEqualTo(ContractType.ITEM_RATE);
    }

    @Test
    void createProject_withNoContractType_defaultsToTurnkey() {
        CustomerProjectCreateRequest req = minimalRequest(null);
        CustomerProject saved = service.createProject(req, "test@wd.com");
        assertThat(saved.getContractType()).isEqualTo(ContractType.TURNKEY);
    }

    // ── rejection tests ────────────────────────────────────────────────────────

    @Test
    void createProject_withCostPlus_isRejected() {
        assertThatThrownBy(() -> service.createProject(minimalRequest("COST_PLUS"), "test@wd.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("COST_PLUS")
                .hasMessageContaining("TURNKEY")
                .hasMessageContaining("ITEM_RATE");
    }

    @Test
    void createProject_withLaborOnly_isRejected() {
        assertThatThrownBy(() -> service.createProject(minimalRequest("LABOR_ONLY"), "test@wd.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("LABOR_ONLY")
                .hasMessageContaining("TURNKEY")
                .hasMessageContaining("ITEM_RATE");
    }

    @Test
    void createProject_withUnknownContractType_isRejected() {
        assertThatThrownBy(() -> service.createProject(minimalRequest("FIXED_PRICE"), "test@wd.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("FIXED_PRICE");
    }

    // ── backward-compat: existing rows with deprecated types still map ──────────

    @Test
    void contractType_laborOnly_canBeSetOnEntityDirectly_forExistingRowCompat() {
        // This confirms the enum constant still exists so JPA can map it from DB
        CustomerProject p = new CustomerProject();
        p.setContractType(ContractType.LABOR_ONLY);
        assertThat(p.getContractType()).isEqualTo(ContractType.LABOR_ONLY);
    }

    @Test
    void contractType_costPlus_canBeSetOnEntityDirectly_forExistingRowCompat() {
        CustomerProject p = new CustomerProject();
        p.setContractType(ContractType.COST_PLUS);
        assertThat(p.getContractType()).isEqualTo(ContractType.COST_PLUS);
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private CustomerProjectCreateRequest minimalRequest(String contractType) {
        CustomerProjectCreateRequest req = new CustomerProjectCreateRequest();
        req.setName("Test Project");
        req.setLocation("Kochi");
        req.setStartDate(LocalDate.of(2026, 6, 1));
        req.setEndDate(LocalDate.of(2027, 6, 1));
        req.setContractType(contractType);
        return req;
    }
}
