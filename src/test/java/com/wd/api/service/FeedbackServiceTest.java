package com.wd.api.service;

import com.wd.api.model.FeedbackResponse;
import com.wd.api.repository.FeedbackResponseRepository;
import com.wd.api.repository.FeedbackFormRepository;
import com.wd.api.repository.CustomerProjectRepository;
import com.wd.api.repository.PortalUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * TDD slice for the admin reply feature in {@link FeedbackService} (audit Card 4.13).
 *
 * <p>Guarantees:
 * <ol>
 *   <li>{@code replyToResponse} sets adminResponse, adminRespondedAt, adminRespondedById
 *       on the entity and persists it.</li>
 *   <li>Unknown responseId throws {@link RuntimeException}.</li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
class FeedbackServiceTest {

    @Mock private FeedbackFormRepository feedbackFormRepository;
    @Mock private FeedbackResponseRepository feedbackResponseRepository;
    @Mock private CustomerProjectRepository projectRepository;
    @Mock private PortalUserRepository portalUserRepository;

    @InjectMocks
    private FeedbackService feedbackService;

    // ─── replyToResponse — sets three fields and saves ──────────────────────────

    @Test
    void replyToResponse_setsAllThreeFieldsAndSaves() {
        FeedbackResponse existing = new FeedbackResponse();
        existing.setId(55L);

        when(feedbackResponseRepository.findById(55L)).thenReturn(Optional.of(existing));
        when(feedbackResponseRepository.save(any(FeedbackResponse.class))).thenAnswer(inv -> inv.getArgument(0));

        feedbackService.replyToResponse(55L, "Thank you for your feedback!", 7L);

        ArgumentCaptor<FeedbackResponse> captor = ArgumentCaptor.forClass(FeedbackResponse.class);
        verify(feedbackResponseRepository).save(captor.capture());

        FeedbackResponse saved = captor.getValue();
        assertThat(saved.getAdminResponse()).isEqualTo("Thank you for your feedback!");
        assertThat(saved.getAdminRespondedAt()).isNotNull();
        assertThat(saved.getAdminRespondedById()).isEqualTo(7L);
    }

    // ─── replyToResponse — not found throws ─────────────────────────────────────

    @Test
    void replyToResponse_unknownResponseId_throwsRuntimeException() {
        when(feedbackResponseRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> feedbackService.replyToResponse(999L, "hello", 1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("999");

        verify(feedbackResponseRepository, never()).save(any());
    }
}
