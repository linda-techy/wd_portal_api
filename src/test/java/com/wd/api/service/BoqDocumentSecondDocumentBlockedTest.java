package com.wd.api.service;

import com.wd.api.model.CustomerProject;
import com.wd.api.model.enums.BoqDocumentStatus;
import com.wd.api.repository.*;
import com.wd.api.security.ProjectAccessGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Regression-lock for audit card 4.4 / Method-2 guarantee (R-003):
 * once a project has an APPROVED BOQ document, {@link BoqDocumentService#createDocument}
 * must throw, blocking any second BOQ. All scope changes must go through a Change Order.
 *
 * This is a lightweight Mockito unit test that stubs the single repository call
 * ({@code existsByProjectIdAndStatus}) that enforces the guard, without needing
 * a running database.
 */
@ExtendWith(MockitoExtension.class)
class BoqDocumentSecondDocumentBlockedTest {

    @Mock private BoqDocumentRepository boqDocumentRepository;
    @Mock private BoqItemRepository boqItemRepository;
    @Mock private PaymentStageRepository paymentStageRepository;
    @Mock private CustomerProjectRepository projectRepository;
    @Mock private PortalUserRepository portalUserRepository;
    @Mock private ProjectAccessGuard projectAccessGuard;
    @Mock private CustomerUserRepository customerUserRepository;
    @Mock private ActivityFeedService activityFeedService;
    @Mock private ProjectStageTemplateRepository stageTemplateRepository;

    @InjectMocks
    private BoqDocumentService boqDocumentService;

    private static final Long PROJECT_ID = 42L;
    private static final Long USER_ID = 1L;

    @BeforeEach
    void setUp() {
        CustomerProject project = new CustomerProject();
        project.setId(PROJECT_ID);

        // The service first loads the project, then checks for an APPROVED BOQ.
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));

        // Simulate: the project already has an APPROVED BOQ document.
        when(boqDocumentRepository.existsByProjectIdAndStatus(PROJECT_ID, BoqDocumentStatus.APPROVED))
                .thenReturn(true);

        // projectAccessGuard is a void-method; the default Mockito behaviour (do nothing) is correct —
        // we want the guard to pass so the APPROVED-BOQ check is reached.
    }

    /**
     * A second call to createDocument on a project that already owns an APPROVED BOQ
     * must be rejected with an IllegalStateException whose message references Change Orders.
     */
    @Test
    void createDocument_whenApprovedBoqAlreadyExists_throwsIllegalStateException() {
        assertThatThrownBy(() ->
                boqDocumentService.createDocument(PROJECT_ID, BigDecimal.ZERO, USER_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("All scope changes must be submitted as Change Orders");
    }
}
