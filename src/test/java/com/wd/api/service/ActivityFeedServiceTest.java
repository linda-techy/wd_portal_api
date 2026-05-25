package com.wd.api.service;

import com.wd.api.model.ActivityFeed;
import com.wd.api.model.ActivityType;
import com.wd.api.repository.ActivityFeedRepository;
import com.wd.api.repository.ActivityTypeRepository;
import com.wd.api.repository.LeadInteractionRepository;
import com.wd.api.repository.LeadRepository;
import com.wd.api.repository.PortalUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * TDD guard for audit Card 4.1: a missing activity_type seed must NOT roll back
 * the caller's primary business write.
 *
 * <p>ActivityFeedService uses @Autowired field injection; Mockito's @InjectMocks
 * handles that via field injection automatically.
 */
@ExtendWith(MockitoExtension.class)
class ActivityFeedServiceTest {

    @Mock private ActivityFeedRepository activityFeedRepository;
    @Mock private ActivityTypeRepository activityTypeRepository;
    @Mock private LeadInteractionRepository leadInteractionRepository;
    @Mock private PortalUserRepository portalUserRepository;
    @Mock private LeadRepository leadRepository;

    @InjectMocks
    private ActivityFeedService activityFeedService;

    /**
     * Test 1 (the lock): when the activity type name has no seed row, logSystemActivity
     * must NOT throw (i.e. it must not propagate the missing-type error and roll back
     * the caller's transaction), and it must NOT attempt to persist anything.
     *
     * This test is RED before the fix (orElseThrow fires) and GREEN after.
     */
    @Test
    void logSystemActivity_withUnseededTypeName_doesNotThrowAndDoesNotSave() {
        when(activityTypeRepository.findByName("UNSEEDED_TYPE"))
                .thenReturn(Optional.empty());

        assertThatNoException().isThrownBy(() ->
                activityFeedService.logSystemActivity(
                        "UNSEEDED_TYPE", "title", "desc", 1L, "SYSTEM"));

        verify(activityFeedRepository, never()).save(any());
    }

    /**
     * Test 2 (happy path intact): when the activity type IS found, logSystemActivity
     * must persist exactly one ActivityFeed. referenceType=SYSTEM means no lead/project
     * lookups run, so no additional stubs are needed.
     */
    @Test
    void logSystemActivity_withKnownTypeName_savesOneActivityFeed() {
        ActivityType activityType = new ActivityType();
        activityType.setName("BOQ_APPROVED");

        when(activityTypeRepository.findByName("BOQ_APPROVED"))
                .thenReturn(Optional.of(activityType));

        activityFeedService.logSystemActivity(
                "BOQ_APPROVED", "BOQ approved", "BOQ was approved", 99L, "SYSTEM");

        verify(activityFeedRepository).save(any(ActivityFeed.class));
    }
}
