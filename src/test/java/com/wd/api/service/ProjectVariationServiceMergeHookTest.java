package com.wd.api.service;

import com.wd.api.dto.changerequest.ChangeRequestMergeResult;
import com.wd.api.model.ProjectVariation;
import com.wd.api.model.enums.VariationStatus;
import com.wd.api.repository.ChangeRequestApprovalHistoryRepository;
import com.wd.api.repository.CustomerProjectRepository;
import com.wd.api.repository.PortalUserRepository;
import com.wd.api.repository.ProjectVariationRepository;
import com.wd.api.service.changerequest.ChangeRequestMergeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Verifies S4 PR2's merge-hook into ProjectVariationService.schedule:
 *
 * <ul>
 *   <li>schedule (APPROVED -> SCHEDULED) invokes ChangeRequestMergeService.mergeIntoWbs</li>
 *   <li>If the merge throws, the exception propagates (Spring rolls back the
 *       transaction in production; verified at the integration layer).</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class ProjectVariationServiceMergeHookTest {

    @Mock private ProjectVariationRepository variationRepository;
    @Mock private CustomerProjectRepository projectRepository;
    @Mock private PortalUserRepository portalUserRepository;
    @Mock private ChangeRequestApprovalHistoryRepository historyRepository;
    @Mock private ChangeRequestMergeService mergeService;

    @InjectMocks private ProjectVariationService service;

    private ProjectVariation cr;

    @BeforeEach
    void setUp() {
        cr = ProjectVariation.builder().id(42L).status(VariationStatus.APPROVED).build();
        when(variationRepository.findById(42L)).thenReturn(Optional.of(cr));
        lenient().when(variationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(mergeService.mergeIntoWbs(eq(42L), eq(500L), anyLong()))
                .thenReturn(new ChangeRequestMergeResult(3, 3, 4, true));
    }

    @Test
    void schedule_invokesMergeServiceWithGivenAnchor() {
        service.schedule(42L, 500L, 99L);
        verify(mergeService).mergeIntoWbs(42L, 500L, 99L);
    }

    @Test
    void schedule_propagatesMergeFailureForRollback() {
        when(mergeService.mergeIntoWbs(anyLong(), anyLong(), anyLong()))
                .thenThrow(new IllegalStateException("cycle"));

        assertThatThrownBy(() -> service.schedule(42L, 500L, 99L))
                .isInstanceOf(IllegalStateException.class);
    }
}
