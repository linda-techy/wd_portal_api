package com.wd.api.service.changerequest;

import com.wd.api.dto.changerequest.ChangeRequestTaskCreateRequest;
import com.wd.api.dto.changerequest.ChangeRequestTaskPredecessorRequest;
import com.wd.api.dto.changerequest.ChangeRequestTaskUpdateRequest;
import com.wd.api.model.ProjectVariation;
import com.wd.api.model.changerequest.ChangeRequestTask;
import com.wd.api.model.changerequest.ChangeRequestTaskPredecessor;
import com.wd.api.model.enums.FloorLoop;
import com.wd.api.model.enums.VariationStatus;
import com.wd.api.repository.ProjectVariationRepository;
import com.wd.api.repository.changerequest.ChangeRequestTaskPredecessorRepository;
import com.wd.api.repository.changerequest.ChangeRequestTaskRepository;
import com.wd.api.service.scheduling.TaskGraphValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChangeRequestTaskServiceTest {

    @Mock private ChangeRequestTaskRepository taskRepo;
    @Mock private ChangeRequestTaskPredecessorRepository predRepo;
    @Mock private ProjectVariationRepository crRepo;

    @InjectMocks private ChangeRequestTaskService service;

    private ProjectVariation cr;

    @BeforeEach
    void setUp() {
        cr = ProjectVariation.builder().id(42L).status(VariationStatus.DRAFT).build();
        when(crRepo.findById(42L)).thenReturn(Optional.of(cr));
    }

    @Test
    void addTask_appendsWithNextSequence() {
        when(taskRepo.countByChangeRequestId(42L)).thenReturn(2L);
        when(taskRepo.save(any())).thenAnswer(inv -> {
            ChangeRequestTask t = inv.getArgument(0);
            t.setId(7L);
            return t;
        });

        ChangeRequestTaskCreateRequest req = new ChangeRequestTaskCreateRequest();
        req.setName("Add living-room slab");
        req.setDurationDays(3);
        req.setFloorLoop(FloorLoop.NONE);

        ChangeRequestTask saved = service.addTask(42L, req, /*actorUserId*/ 99L);

        assertThat(saved.getSequence()).isEqualTo(3);
        assertThat(saved.getName()).isEqualTo("Add living-room slab");
        assertThat(saved.getChangeRequest()).isSameAs(cr);
    }

    @Test
    void addTask_rejects_whenCrIsCosted_withConflict() {
        cr.setStatus(VariationStatus.COSTED);
        ChangeRequestTaskCreateRequest req = new ChangeRequestTaskCreateRequest();
        req.setName("X"); req.setDurationDays(1);

        assertThatThrownBy(() -> service.addTask(42L, req, 99L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("frozen");
    }

    @Test
    void updateTask_rejects_whenCrIsApproved() {
        cr.setStatus(VariationStatus.APPROVED);
        ChangeRequestTaskUpdateRequest req = new ChangeRequestTaskUpdateRequest();
        req.setName("New");

        assertThatThrownBy(() -> service.updateTask(42L, 7L, req, 99L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("frozen");
    }

    @Test
    void updateTask_inDraft_appliesPatchSemantics() {
        ChangeRequestTask existing = new ChangeRequestTask();
        existing.setId(7L);
        existing.setChangeRequest(cr);
        existing.setName("Old name");
        existing.setDurationDays(1);
        existing.setMonsoonSensitive(false);
        when(taskRepo.findById(7L)).thenReturn(Optional.of(existing));
        when(taskRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ChangeRequestTaskUpdateRequest req = new ChangeRequestTaskUpdateRequest();
        req.setName("New name");
        // durationDays NOT supplied — must remain 1.

        ChangeRequestTask updated = service.updateTask(42L, 7L, req, 99L);
        assertThat(updated.getName()).isEqualTo("New name");
        assertThat(updated.getDurationDays()).isEqualTo(1);
    }

    @Test
    void removeTask_rejects_whenCrIsScheduled() {
        cr.setStatus(VariationStatus.SCHEDULED);

        assertThatThrownBy(() -> service.removeTask(42L, 7L, 99L))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void addPredecessor_rejects_whenEndpointsBelongToDifferentCRs() {
        when(taskRepo.existsByIdAndChangeRequestId(7L, 42L)).thenReturn(true);
        when(taskRepo.existsByIdAndChangeRequestId(8L, 42L)).thenReturn(false);

        ChangeRequestTaskPredecessorRequest req = new ChangeRequestTaskPredecessorRequest();
        req.setSuccessorCrTaskId(7L);
        req.setPredecessorCrTaskId(8L);

        assertThatThrownBy(() -> service.addPredecessor(42L, req, 99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("same CR");
    }

    @Test
    void addPredecessor_rejects_selfLoop() {
        when(taskRepo.existsByIdAndChangeRequestId(7L, 42L)).thenReturn(true);

        ChangeRequestTaskPredecessorRequest req = new ChangeRequestTaskPredecessorRequest();
        req.setSuccessorCrTaskId(7L);
        req.setPredecessorCrTaskId(7L);

        assertThatThrownBy(() -> service.addPredecessor(42L, req, 99L))
                .isInstanceOf(TaskGraphValidator.CycleDetectedException.class);
    }

    @Test
    void addPredecessor_rejects_whenWouldCloseCycle() {
        when(taskRepo.existsByIdAndChangeRequestId(7L, 42L)).thenReturn(true);
        when(taskRepo.existsByIdAndChangeRequestId(8L, 42L)).thenReturn(true);
        // 8 already depends on 7; adding 7-depends-on-8 would close a cycle.
        ChangeRequestTaskPredecessor existing = new ChangeRequestTaskPredecessor(8L, 7L, 0);
        when(predRepo.findBySuccessorCrTaskId(8L)).thenReturn(List.of(existing));

        ChangeRequestTaskPredecessorRequest req = new ChangeRequestTaskPredecessorRequest();
        req.setSuccessorCrTaskId(7L);
        req.setPredecessorCrTaskId(8L);

        assertThatThrownBy(() -> service.addPredecessor(42L, req, 99L))
                .isInstanceOf(TaskGraphValidator.CycleDetectedException.class);
    }

    @Test
    void addPredecessor_persists_whenAcyclic() {
        when(taskRepo.existsByIdAndChangeRequestId(7L, 42L)).thenReturn(true);
        when(taskRepo.existsByIdAndChangeRequestId(8L, 42L)).thenReturn(true);
        when(predRepo.findBySuccessorCrTaskId(any())).thenReturn(List.of());
        when(predRepo.save(any())).thenAnswer(inv -> {
            ChangeRequestTaskPredecessor p = inv.getArgument(0);
            p.setId(11L);
            return p;
        });

        ChangeRequestTaskPredecessorRequest req = new ChangeRequestTaskPredecessorRequest();
        req.setSuccessorCrTaskId(8L);
        req.setPredecessorCrTaskId(7L);
        req.setLagDays(2);

        ChangeRequestTaskPredecessor saved = service.addPredecessor(42L, req, 99L);

        ArgumentCaptor<ChangeRequestTaskPredecessor> cap =
                ArgumentCaptor.forClass(ChangeRequestTaskPredecessor.class);
        org.mockito.Mockito.verify(predRepo).save(cap.capture());
        assertThat(cap.getValue().getSuccessorCrTaskId()).isEqualTo(8L);
        assertThat(cap.getValue().getPredecessorCrTaskId()).isEqualTo(7L);
        assertThat(cap.getValue().getLagDays()).isEqualTo(2);
        assertThat(saved.getId()).isEqualTo(11L);
    }
}
