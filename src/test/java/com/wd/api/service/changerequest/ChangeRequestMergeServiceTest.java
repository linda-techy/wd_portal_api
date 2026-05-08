package com.wd.api.service.changerequest;

import com.wd.api.dto.changerequest.ChangeRequestMergeResult;
import com.wd.api.model.CustomerProject;
import com.wd.api.model.ProjectVariation;
import com.wd.api.model.Task;
import com.wd.api.model.changerequest.ChangeRequestTask;
import com.wd.api.model.changerequest.ChangeRequestTaskPredecessor;
import com.wd.api.model.enums.FloorLoop;
import com.wd.api.model.enums.VariationStatus;
import com.wd.api.model.scheduling.TaskPredecessor;
import com.wd.api.repository.ProjectScheduleConfigRepository;
import com.wd.api.repository.ProjectVariationRepository;
import com.wd.api.repository.TaskPredecessorRepository;
import com.wd.api.repository.TaskRepository;
import com.wd.api.repository.changerequest.ChangeRequestTaskPredecessorRepository;
import com.wd.api.repository.changerequest.ChangeRequestTaskRepository;
import com.wd.api.service.scheduling.CpmService;
import com.wd.api.service.scheduling.HandoverShiftDetector;
import com.wd.api.service.scheduling.HolidayService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChangeRequestMergeServiceTest {

    @Mock private ChangeRequestTaskRepository crTaskRepo;
    @Mock private ChangeRequestTaskPredecessorRepository crPredRepo;
    @Mock private ProjectVariationRepository crRepo;
    @Mock private TaskRepository taskRepo;
    @Mock private TaskPredecessorRepository taskPredRepo;
    @Mock private ProjectScheduleConfigRepository scheduleConfigRepo;
    @Mock private HolidayService holidayService;
    @Mock private CpmService cpmService;
    @Mock private HandoverShiftDetector handoverShiftDetector;

    @InjectMocks private ChangeRequestMergeService service;

    private CustomerProject project;
    private Task anchor;
    private ProjectVariation cr;

    @BeforeEach
    void setUp() {
        project = new CustomerProject();
        project.setId(11L);
        project.setFloors(2);

        anchor = new Task();
        anchor.setId(500L);
        anchor.setProject(project);
        anchor.setMilestoneId(33L);
        anchor.setTitle("Anchor");
        anchor.setStatus(Task.TaskStatus.PENDING);

        cr = ProjectVariation.builder()
                .id(42L)
                .project(project)
                .description("Add 2 rooms")
                .status(VariationStatus.APPROVED)
                .build();

        lenient().when(crRepo.findById(42L)).thenReturn(Optional.of(cr));
        lenient().when(taskRepo.findById(500L)).thenReturn(Optional.of(anchor));

        // Auto-id task and predecessor saves so the merge can build its idMap.
        AtomicLong nextTaskId = new AtomicLong(1000L);
        lenient().when(taskRepo.save(any(Task.class))).thenAnswer(inv -> {
            Task t = inv.getArgument(0);
            if (t.getId() == null) t.setId(nextTaskId.getAndIncrement());
            return t;
        });
        lenient().when(taskPredRepo.save(any(TaskPredecessor.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void linearChainOfThreeProposed_yieldsThreeTasksTwoPredsOneAnchor() {
        ChangeRequestTask a = crTask(101L, 1, "A", FloorLoop.NONE);
        ChangeRequestTask b = crTask(102L, 2, "B", FloorLoop.NONE);
        ChangeRequestTask c = crTask(103L, 3, "C", FloorLoop.NONE);
        when(crTaskRepo.findByChangeRequestIdOrderBySequenceAsc(42L))
                .thenReturn(List.of(a, b, c));

        // a -> b -> c (2 edges in CR-task land)
        ChangeRequestTaskPredecessor e1 = new ChangeRequestTaskPredecessor(102L, 101L, 0);
        ChangeRequestTaskPredecessor e2 = new ChangeRequestTaskPredecessor(103L, 102L, 0);
        when(crPredRepo.findByChangeRequestId(42L)).thenReturn(List.of(e1, e2));

        ChangeRequestMergeResult result = service.mergeIntoWbs(42L, 500L, /*actorUserId*/ 99L);

        assertThat(result.tasksCreated()).isEqualTo(3);
        assertThat(result.proposedTasksCount()).isEqualTo(3);
        // 2 internal edges + 1 anchor edge = 3
        assertThat(result.predecessorEdgesCreated()).isEqualTo(3);

        ArgumentCaptor<TaskPredecessor> predCap =
                ArgumentCaptor.forClass(TaskPredecessor.class);
        verify(taskPredRepo, times(3)).save(predCap.capture());

        // The anchor predecessor is the one whose predecessorId == anchor.id (500).
        boolean anchorEdgePresent = predCap.getAllValues().stream()
                .anyMatch(p -> p.getPredecessorId().equals(500L));
        assertThat(anchorEdgePresent).isTrue();
    }

    @Test
    void perFloorProposedTask_expandsAcrossProjectFloors() {
        // Project has floors=2 (set in @BeforeEach).
        ChangeRequestTask a = crTask(101L, 1, "Slab", FloorLoop.PER_FLOOR);
        when(crTaskRepo.findByChangeRequestIdOrderBySequenceAsc(42L))
                .thenReturn(List.of(a));
        when(crPredRepo.findByChangeRequestId(42L)).thenReturn(List.of());

        ChangeRequestMergeResult result = service.mergeIntoWbs(42L, 500L, 99L);

        // 1 PER_FLOOR proposed task * 2 floors = 2 cloned Tasks.
        assertThat(result.tasksCreated()).isEqualTo(2);

        // Each cloned floor instance gets the anchor as its predecessor → 2 anchor edges.
        verify(taskPredRepo, times(2)).save(argThat(p -> p.getPredecessorId().equals(500L)));
    }

    @Test
    void timeImpactGreaterThanZero_invokesDelayApplier_thenCpmThenHandover() {
        cr.setTimeImpactWorkingDays(7);
        ChangeRequestTask only = crTask(101L, 1, "X", FloorLoop.NONE);
        when(crTaskRepo.findByChangeRequestIdOrderBySequenceAsc(42L)).thenReturn(List.of(only));
        when(crPredRepo.findByChangeRequestId(42L)).thenReturn(List.of());
        when(scheduleConfigRepo.findByProjectId(11L)).thenReturn(Optional.empty());
        when(taskRepo.findByProjectId(11L)).thenReturn(List.of());

        // We're testing the call-graph; DelayApplier internals already covered by S3 PR3 tests.
        // The merge service constructs a transient DelayLog; we verify CpmService + Handover
        // are invoked AFTER the time-impact application via InOrder against the latter two.
        service.mergeIntoWbs(42L, 500L, 99L);

        InOrder ord = inOrder(cpmService, handoverShiftDetector);
        ord.verify(cpmService).recompute(11L);
        ord.verify(handoverShiftDetector).checkAndAlert(11L);
    }

    @Test
    void timeImpactZero_skipsDelayApplier_butStillRecomputesAndChecks() {
        cr.setTimeImpactWorkingDays(0);
        ChangeRequestTask only = crTask(101L, 1, "X", FloorLoop.NONE);
        when(crTaskRepo.findByChangeRequestIdOrderBySequenceAsc(42L)).thenReturn(List.of(only));
        when(crPredRepo.findByChangeRequestId(42L)).thenReturn(List.of());

        service.mergeIntoWbs(42L, 500L, 99L);

        // No findByProjectId call from DelayApplier path.
        verify(taskRepo, never()).findByProjectId(11L);
        // CPM + handover still ran (new tasks may shift the critical path).
        verify(cpmService).recompute(11L);
        verify(handoverShiftDetector).checkAndAlert(11L);
    }

    @Test
    void handoverShiftDetectorCalledStrictlyAfterCpmRecompute() {
        cr.setTimeImpactWorkingDays(0);
        ChangeRequestTask only = crTask(101L, 1, "X", FloorLoop.NONE);
        when(crTaskRepo.findByChangeRequestIdOrderBySequenceAsc(42L)).thenReturn(List.of(only));
        when(crPredRepo.findByChangeRequestId(42L)).thenReturn(List.of());

        service.mergeIntoWbs(42L, 500L, 99L);

        InOrder ord = inOrder(cpmService, handoverShiftDetector);
        ord.verify(cpmService).recompute(11L);
        ord.verify(handoverShiftDetector).checkAndAlert(11L);
    }

    @Test
    void costImpactDoesNotMutateProjectBudget() {
        cr.setCostImpact(new BigDecimal("125000"));
        cr.setTimeImpactWorkingDays(0);
        ChangeRequestTask only = crTask(101L, 1, "X", FloorLoop.NONE);
        when(crTaskRepo.findByChangeRequestIdOrderBySequenceAsc(42L)).thenReturn(List.of(only));
        when(crPredRepo.findByChangeRequestId(42L)).thenReturn(List.of());

        service.mergeIntoWbs(42L, 500L, 99L);

        // The merge writes nothing to project_variations beyond what came in
        // through ProjectVariationService.schedule (PR1); cost impact lives on
        // the CR row only. Verify by spying on crRepo: no save invoked.
        verify(crRepo, never()).save(any());
    }

    @Test
    void nonApprovedCr_throwsIllegalState() {
        cr.setStatus(VariationStatus.COSTED);
        assertThatThrownBy(() -> service.mergeIntoWbs(42L, 500L, 99L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("APPROVED");
        verifyNoInteractions(cpmService, handoverShiftDetector);
        // taskRepo may be touched by findById — anchor lookup happens AFTER CR
        // status validation, so verify it stayed quiet too.
        verify(taskRepo, never()).save(any());
    }

    @Test
    void mergeRequiresAnchorTaskToBelongToSameProject() {
        Task foreignAnchor = new Task();
        foreignAnchor.setId(999L);
        CustomerProject other = new CustomerProject();
        other.setId(99L);
        foreignAnchor.setProject(other);
        when(taskRepo.findById(999L)).thenReturn(Optional.of(foreignAnchor));

        assertThatThrownBy(() -> service.mergeIntoWbs(42L, 999L, 99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("anchor");
    }

    @Test
    void clonedTasksCarryDurationDaysWeightAndMonsoonSensitive() {
        ChangeRequestTask a = new ChangeRequestTask();
        a.setId(101L); a.setSequence(1); a.setName("A");
        a.setDurationDays(7);
        a.setWeightFactor(5);
        a.setMonsoonSensitive(true);
        a.setIsPaymentMilestone(true);            // explicitly NOT copied to Task
        a.setFloorLoop(FloorLoop.NONE);
        a.setChangeRequest(cr);
        when(crTaskRepo.findByChangeRequestIdOrderBySequenceAsc(42L)).thenReturn(List.of(a));
        when(crPredRepo.findByChangeRequestId(42L)).thenReturn(List.of());

        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        service.mergeIntoWbs(42L, 500L, 99L);
        verify(taskRepo, atLeastOnce()).save(captor.capture());
        Task cloned = captor.getAllValues().get(0);
        assertThat(cloned.getDurationDays()).isEqualTo(7);
        assertThat(cloned.getWeight()).isEqualTo(5);
        assertThat(cloned.getMonsoonSensitive()).isTrue();
        assertThat(cloned.getMilestoneId()).isEqualTo(33L);
        assertThat(cloned.getProject().getId()).isEqualTo(11L);
        assertThat(cloned.getStatus()).isEqualTo(Task.TaskStatus.PENDING);
        // is_payment_milestone is not a column on Task; nothing to assert here.
    }

    private ChangeRequestTask crTask(Long id, int seq, String name, FloorLoop floor) {
        ChangeRequestTask t = new ChangeRequestTask();
        t.setId(id);
        t.setChangeRequest(cr);
        t.setSequence(seq);
        t.setName(name);
        t.setDurationDays(1);
        t.setFloorLoop(floor);
        return t;
    }
}
