package com.wd.api.service;

import com.wd.api.model.ChangeOrder;
import com.wd.api.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChangeOrderVoListSeparationTest {

    // --- shared dependency ---
    @Mock ChangeOrderRepository changeOrderRepository;

    // --- ChangeOrderService additional dependencies ---
    @Mock BoqDocumentRepository boqDocumentRepository;
    @Mock CustomerProjectRepository projectRepository;
    @Mock PortalUserRepository portalUserRepository;
    @Mock CreditNoteService creditNoteService;
    @Mock BoqInvoiceService boqInvoiceService;
    @Mock ActivityFeedService activityFeedService;
    @Mock CustomerNotificationFacade customerNotificationFacade;

    // --- VariationOrderService additional dependencies ---
    @Mock ChangeOrderApprovalHistoryRepository approvalHistoryRepository;
    @Mock ChangeOrderPaymentScheduleRepository paymentScheduleRepository;
    @Mock ApplicationEventPublisher eventPublisher;

    @Test
    void getProjectChangeOrders_returnsOnlyRegularCos_voCategoryIsNull() {
        when(changeOrderRepository
                .findByProjectIdAndVoCategoryIsNullAndDeletedAtIsNullOrderByCreatedAtDesc(50L))
                .thenReturn(List.<ChangeOrder>of());

        newChangeOrderService().getProjectChangeOrders(50L);

        verify(changeOrderRepository)
                .findByProjectIdAndVoCategoryIsNullAndDeletedAtIsNullOrderByCreatedAtDesc(50L);
        verify(changeOrderRepository, never())
                .findByProjectIdAndDeletedAtIsNullOrderByCreatedAtDesc(eq(50L));
    }

    @Test
    void variationOrderList_returnsOnlyVos_voCategoryIsNotNull() {
        when(changeOrderRepository
                .findByProjectIdAndVoCategoryIsNotNullAndDeletedAtIsNullOrderByCreatedAtDesc(50L))
                .thenReturn(List.<ChangeOrder>of());

        newVariationOrderService().listByProject(50L);

        verify(changeOrderRepository)
                .findByProjectIdAndVoCategoryIsNotNullAndDeletedAtIsNullOrderByCreatedAtDesc(50L);
        verify(changeOrderRepository, never())
                .findByProjectIdAndDeletedAtIsNullOrderByCreatedAtDesc(eq(50L));
    }

    private ChangeOrderService newChangeOrderService() {
        return new ChangeOrderService(
                changeOrderRepository,
                boqDocumentRepository,
                projectRepository,
                portalUserRepository,
                creditNoteService,
                boqInvoiceService,
                activityFeedService,
                customerNotificationFacade);
    }

    private VariationOrderService newVariationOrderService() {
        return new VariationOrderService(
                changeOrderRepository,
                approvalHistoryRepository,
                paymentScheduleRepository,
                boqDocumentRepository,
                projectRepository,
                portalUserRepository,
                eventPublisher);
    }
}
