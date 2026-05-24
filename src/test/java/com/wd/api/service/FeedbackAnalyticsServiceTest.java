package com.wd.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wd.api.dto.FeedbackAnalyticsDto;
import com.wd.api.model.FeedbackForm;
import com.wd.api.model.FeedbackResponse;
import com.wd.api.repository.FeedbackFormRepository;
import com.wd.api.repository.FeedbackResponseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * TDD slice for {@link FeedbackAnalyticsService}.
 *
 * <p>Schema convention (enforced by this service):
 * <ul>
 *   <li>{@code formSchema} is a JSON array of question definitions:
 *       {@code [{"key","label","type"}]} where {@code type} is one of
 *       {@code rating} (integer 1-5), {@code nps} (integer 0-10),
 *       {@code text} (free-form string), or {@code choice} (string).</li>
 *   <li>{@code responseData} is a JSON object keyed by question key, e.g.
 *       {@code {"q1":4,"q2":"Loved the transparency"}}.</li>
 *   <li>When {@code formSchema} is null/empty the service synthesises
 *       numeric questions dynamically from the response data keys.</li>
 * </ul>
 *
 * <p>Service guarantees verified here:
 * <ol>
 *   <li>Two rating questions produce correct per-question average, count, min,
 *       max, and value distribution.</li>
 *   <li>Text/non-numeric questions are excluded from numeric stats.</li>
 *   <li>Zero responses returns a safe empty summary (no divide-by-zero).</li>
 *   <li>Malformed / null {@code responseData} rows are skipped gracefully.</li>
 *   <li>NPS is computed only when an {@code nps}-type question is present.</li>
 *   <li>Unknown form ID throws {@link IllegalArgumentException}.</li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
class FeedbackAnalyticsServiceTest {

    @Mock private FeedbackFormRepository formRepository;
    @Mock private FeedbackResponseRepository responseRepository;

    private FeedbackAnalyticsService service;

    // Two rating questions + one text question
    private static final String SCHEMA_TWO_RATING_ONE_TEXT =
            "[" +
            "{\"key\":\"overall\",\"label\":\"Overall satisfaction\",\"type\":\"rating\"}," +
            "{\"key\":\"quality\",\"label\":\"Build quality\",\"type\":\"rating\"}," +
            "{\"key\":\"comments\",\"label\":\"Comments\",\"type\":\"text\"}" +
            "]";

    // Schema with NPS question
    private static final String SCHEMA_WITH_NPS =
            "[{\"key\":\"recommend\",\"label\":\"Recommend to a friend (0-10)\",\"type\":\"nps\"}]";

    @BeforeEach
    void setUp() {
        service = new FeedbackAnalyticsService(formRepository, responseRepository, new ObjectMapper());
    }

    // ── helper ────────────────────────────────────────────────────────────────

    private FeedbackForm makeForm(Long id, String schema) {
        FeedbackForm f = new FeedbackForm();
        ReflectionTestUtils.setField(f, "id", id);
        f.setTitle("Test Form");
        f.setFormSchema(schema);
        return f;
    }

    private FeedbackResponse makeResponse(String responseDataJson) {
        FeedbackResponse r = new FeedbackResponse();
        r.setResponseData(responseDataJson);
        return r;
    }

    // ── two rating questions, N responses ─────────────────────────────────────

    @Test
    void analyseForm_twoRatingQuestions_correctAverageCountDistribution() {
        FeedbackForm form = makeForm(1L, SCHEMA_TWO_RATING_ONE_TEXT);
        when(formRepository.findById(1L)).thenReturn(Optional.of(form));

        List<FeedbackResponse> responses = List.of(
                makeResponse("{\"overall\":5,\"quality\":4,\"comments\":\"Great work\"}"),
                makeResponse("{\"overall\":3,\"quality\":4,\"comments\":\"Average\"}"),
                makeResponse("{\"overall\":4,\"quality\":5,\"comments\":\"Good\"}")
        );
        when(responseRepository.findByFormIdOrderBySubmittedAtDesc(1L)).thenReturn(responses);

        FeedbackAnalyticsDto result = service.analyseForm(1L);

        assertThat(result.formId()).isEqualTo(1L);
        assertThat(result.totalResponses()).isEqualTo(3);

        // "overall": values 5,3,4 -> avg=4.0, min=3, max=5
        FeedbackAnalyticsDto.QuestionStat overall = findStat(result, "overall");
        assertThat(overall.type()).isEqualTo("rating");
        assertThat(overall.count()).isEqualTo(3);
        assertThat(overall.average()).isEqualTo(4.0);
        assertThat(overall.min()).isEqualTo(3.0);
        assertThat(overall.max()).isEqualTo(5.0);
        assertThat(overall.distribution()).containsEntry(3, 1L)
                                          .containsEntry(4, 1L)
                                          .containsEntry(5, 1L);

        // "quality": values 4,4,5 -> avg~4.33, min=4, max=5
        FeedbackAnalyticsDto.QuestionStat quality = findStat(result, "quality");
        assertThat(quality.count()).isEqualTo(3);
        assertThat(quality.average()).isCloseTo(4.333, org.assertj.core.data.Offset.offset(0.01));
        assertThat(quality.min()).isEqualTo(4.0);
        assertThat(quality.max()).isEqualTo(5.0);
        assertThat(quality.distribution()).containsEntry(4, 2L)
                                          .containsEntry(5, 1L);

        // NPS must be absent -- no nps question in this schema
        assertThat(result.nps()).isNull();
    }

    @Test
    void analyseForm_textQuestionsAreIgnoredInPerQuestionStats() {
        FeedbackForm form = makeForm(2L, SCHEMA_TWO_RATING_ONE_TEXT);
        when(formRepository.findById(2L)).thenReturn(Optional.of(form));
        when(responseRepository.findByFormIdOrderBySubmittedAtDesc(2L)).thenReturn(List.of(
                makeResponse("{\"overall\":5,\"quality\":5,\"comments\":\"Excellent\"}")));

        FeedbackAnalyticsDto result = service.analyseForm(2L);

        // text question should not appear in perQuestion stats
        boolean textQuestionPresent = result.perQuestion().stream()
                .anyMatch(q -> "comments".equals(q.key()));
        assertThat(textQuestionPresent).isFalse();

        // only rating questions reported
        assertThat(result.perQuestion()).hasSize(2);
    }

    // ── zero responses ────────────────────────────────────────────────────────

    @Test
    void analyseForm_zeroResponses_returnsSafeEmptySummary() {
        FeedbackForm form = makeForm(3L, SCHEMA_TWO_RATING_ONE_TEXT);
        when(formRepository.findById(3L)).thenReturn(Optional.of(form));
        when(responseRepository.findByFormIdOrderBySubmittedAtDesc(3L)).thenReturn(List.of());

        FeedbackAnalyticsDto result = service.analyseForm(3L);

        assertThat(result.totalResponses()).isZero();
        // stats present but all counts are 0, average is null
        for (FeedbackAnalyticsDto.QuestionStat stat : result.perQuestion()) {
            assertThat(stat.count()).isZero();
            assertThat(stat.average()).isNull();
        }
        assertThat(result.nps()).isNull();
    }

    // ── malformed / null responseData ─────────────────────────────────────────

    @Test
    void analyseForm_malformedResponseDataRowsAreSkippedGracefully() {
        FeedbackForm form = makeForm(4L, SCHEMA_TWO_RATING_ONE_TEXT);
        when(formRepository.findById(4L)).thenReturn(Optional.of(form));

        List<FeedbackResponse> responses = List.of(
                makeResponse(null),                                  // null responseData
                makeResponse("not-valid-json{{{{"),                  // malformed JSON
                makeResponse("{\"overall\":5,\"quality\":4}")        // good row
        );
        when(responseRepository.findByFormIdOrderBySubmittedAtDesc(4L)).thenReturn(responses);

        FeedbackAnalyticsDto result = service.analyseForm(4L);

        // Only 1 valid row contributed stats; malformed rows silently skipped
        FeedbackAnalyticsDto.QuestionStat overall = findStat(result, "overall");
        assertThat(overall.count()).isEqualTo(1);
        assertThat(overall.average()).isEqualTo(5.0);
    }

    // ── NPS question ──────────────────────────────────────────────────────────

    @Test
    void analyseForm_npsQuestion_computesNpsScore() {
        FeedbackForm form = makeForm(5L, SCHEMA_WITH_NPS);
        when(formRepository.findById(5L)).thenReturn(Optional.of(form));

        // NPS: promoters >=9, detractors <=6, passives 7-8
        // 3 promoters (10,10,9), 1 passive (7), 2 detractors (3,4) -> 6 total
        // NPS = (3/6 - 2/6) * 100 = (0.5 - 0.333) * 100 ~= 16.67
        List<FeedbackResponse> responses = List.of(
                makeResponse("{\"recommend\":10}"),
                makeResponse("{\"recommend\":10}"),
                makeResponse("{\"recommend\":9}"),
                makeResponse("{\"recommend\":7}"),
                makeResponse("{\"recommend\":3}"),
                makeResponse("{\"recommend\":4}")
        );
        when(responseRepository.findByFormIdOrderBySubmittedAtDesc(5L)).thenReturn(responses);

        FeedbackAnalyticsDto result = service.analyseForm(5L);

        assertThat(result.nps()).isNotNull();
        // promoters=3, detractors=2, total=6 -> NPS~=16.67
        assertThat(result.nps()).isCloseTo(16.67, org.assertj.core.data.Offset.offset(0.5));
    }

    @Test
    void analyseForm_noNpsQuestion_npsFieldIsNull() {
        FeedbackForm form = makeForm(6L, SCHEMA_TWO_RATING_ONE_TEXT);
        when(formRepository.findById(6L)).thenReturn(Optional.of(form));
        when(responseRepository.findByFormIdOrderBySubmittedAtDesc(6L)).thenReturn(List.of(
                makeResponse("{\"overall\":4,\"quality\":3}")));

        FeedbackAnalyticsDto result = service.analyseForm(6L);

        assertThat(result.nps()).isNull();
    }

    // ── unknown form ──────────────────────────────────────────────────────────

    @Test
    void analyseForm_unknownFormId_throwsIllegalArgumentException() {
        when(formRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.analyseForm(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Feedback form not found");
    }

    // ── null / empty formSchema fallback ──────────────────────────────────────

    @Test
    void analyseForm_nullFormSchema_syntheticRatingFromResponseDataKeys() {
        FeedbackForm form = makeForm(7L, null); // no schema
        when(formRepository.findById(7L)).thenReturn(Optional.of(form));

        FeedbackResponse r1 = makeResponse("{\"overall_rating\":4}");
        FeedbackResponse r2 = makeResponse("{\"overall_rating\":5}");
        when(responseRepository.findByFormIdOrderBySubmittedAtDesc(7L))
                .thenReturn(List.of(r1, r2));

        // When schema is absent the service derives numeric keys from responseData
        FeedbackAnalyticsDto result = service.analyseForm(7L);

        assertThat(result.totalResponses()).isEqualTo(2);
        // At least one stat was derived even without a schema
        assertThat(result.perQuestion()).isNotEmpty();
        FeedbackAnalyticsDto.QuestionStat stat = findStat(result, "overall_rating");
        assertThat(stat.count()).isEqualTo(2);
        assertThat(stat.average()).isEqualTo(4.5);
    }

    // ── project rollup ────────────────────────────────────────────────────────

    @Test
    void analyseProject_aggregatesAcrossForms() {
        FeedbackForm form1 = makeForm(10L, SCHEMA_TWO_RATING_ONE_TEXT);
        FeedbackForm form2 = makeForm(11L, SCHEMA_WITH_NPS);

        when(formRepository.findByProjectIdOrderByCreatedAtDesc(99L))
                .thenReturn(List.of(form1, form2));

        when(responseRepository.findByFormIdOrderBySubmittedAtDesc(10L)).thenReturn(List.of(
                makeResponse("{\"overall\":4,\"quality\":3}")));
        when(responseRepository.findByFormIdOrderBySubmittedAtDesc(11L)).thenReturn(List.of(
                makeResponse("{\"recommend\":9}")));

        FeedbackAnalyticsDto rollup = service.analyseProject(99L);

        assertThat(rollup.totalResponses()).isEqualTo(2);
        assertThat(rollup.formId()).isNull(); // project rollup has no single formId
    }

    // ── private helpers ───────────────────────────────────────────────────────

    private FeedbackAnalyticsDto.QuestionStat findStat(FeedbackAnalyticsDto dto, String key) {
        return dto.perQuestion().stream()
                .filter(q -> key.equals(q.key()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No stat for key: " + key));
    }
}
