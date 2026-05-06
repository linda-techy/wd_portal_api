package com.wd.api.service.scheduling;

import com.wd.api.model.ActivityType;
import com.wd.api.model.CustomerProject;
import com.wd.api.model.Task;
import com.wd.api.repository.ActivityTypeRepository;
import com.wd.api.repository.CustomerProjectRepository;
import com.wd.api.repository.TaskRepository;
import com.wd.api.service.TaskProgressUpdateService;
import com.wd.api.testsupport.TestcontainersPostgresBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@Transactional
class TaskProgressUpdateServiceCpmHookTest extends TestcontainersPostgresBase {

    @Autowired private TaskProgressUpdateService progressService;
    @Autowired private TaskRepository tasks;
    @Autowired private CustomerProjectRepository projects;
    @Autowired private ActivityTypeRepository activityTypes;

    @SpyBean private CpmService cpm;

    @BeforeEach
    void seedActivityType() {
        // updateProgress logs to ActivityFeedService when status changes; the type
        // must exist or the service throws "Runtime Activity Type not found".
        activityTypes.findByName("TASK_STATUS_CHANGED").orElseGet(() -> {
            ActivityType at = new ActivityType();
            at.setName("TASK_STATUS_CHANGED");
            at.setDescription("Task status changed via progress update");
            return activityTypes.save(at);
        });
    }

    @Test
    void progressUpdate_thatTriggersCompletion_callsCpmRecompute() {
        CustomerProject p = newProject();
        Task t = newTask(p, "A");

        progressService.updateProgress(t.getId(), 100, "done", null);

        verify(cpm, atLeastOnce()).recompute(p.getId());
    }

    @Test
    void progressUpdate_thatDoesNotChangeStatus_stillCallsCpmRecompute() {
        // Mirror the unconditional recompute pattern of TaskPredecessorService /
        // GanttService — every save in this service touches actuals/progress and
        // should consistently trigger a recompute, even on intermediate updates
        // (e.g. 25% -> 50%) where status remains IN_PROGRESS and actualEndDate
        // is not auto-stamped. Consistency over micro-optimization.
        CustomerProject p = newProject();
        Task t = newTask(p, "A");

        progressService.updateProgress(t.getId(), 50, "halfway", null);

        verify(cpm, atLeastOnce()).recompute(p.getId());
    }

    private CustomerProject newProject() {
        CustomerProject p = new CustomerProject();
        p.setName("progress-hook " + UUID.randomUUID());
        p.setLocation("Test");
        p.setProjectUuid(UUID.randomUUID());
        p.setStartDate(LocalDate.of(2026, 6, 1));
        return projects.save(p);
    }

    private Task newTask(CustomerProject p, String title) {
        Task t = new Task();
        t.setTitle(title);
        t.setStatus(Task.TaskStatus.PENDING);
        t.setPriority(Task.TaskPriority.MEDIUM);
        t.setProject(p);
        t.setDueDate(LocalDate.of(2030, 12, 31));
        t.setStartDate(LocalDate.of(2026, 6, 1));
        t.setEndDate(LocalDate.of(2026, 6, 5));
        return tasks.save(t);
    }
}
