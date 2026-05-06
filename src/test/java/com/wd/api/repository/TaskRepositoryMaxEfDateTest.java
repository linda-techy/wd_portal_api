package com.wd.api.repository;

import com.wd.api.model.CustomerProject;
import com.wd.api.model.Task;
import com.wd.api.testsupport.TestcontainersPostgresBase;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Slice test for {@link TaskRepository#findMaxEfDateByProjectId(Long)} —
 * mirrors the customer-API method of the same name. Backs
 * {@code HandoverShiftDetector}, which uses the project's max
 * {@code task.ef_date} as the customer-visible expected handover.
 */
@Transactional
class TaskRepositoryMaxEfDateTest extends TestcontainersPostgresBase {

    @Autowired private TaskRepository taskRepository;
    @Autowired private EntityManager em;

    @Test
    void returnsMaxEfDate_whenProjectHasTasksWithEfDates() {
        CustomerProject p = newProject();
        em.persist(p);
        em.persist(newTask(p, LocalDate.of(2026, 7, 1)));
        em.persist(newTask(p, LocalDate.of(2026, 9, 15)));   // max
        em.persist(newTask(p, LocalDate.of(2026, 8, 20)));
        em.flush();

        Optional<LocalDate> max = taskRepository.findMaxEfDateByProjectId(p.getId());
        assertThat(max).contains(LocalDate.of(2026, 9, 15));
    }

    @Test
    void returnsEmpty_whenProjectHasNoTasks() {
        CustomerProject p = newProject();
        em.persist(p);
        em.flush();
        assertThat(taskRepository.findMaxEfDateByProjectId(p.getId())).isEmpty();
    }

    @Test
    void returnsEmpty_whenAllTasksHaveNullEfDate() {
        CustomerProject p = newProject();
        em.persist(p);
        em.persist(newTask(p, null));
        em.flush();
        assertThat(taskRepository.findMaxEfDateByProjectId(p.getId())).isEmpty();
    }

    private CustomerProject newProject() {
        CustomerProject p = new CustomerProject();
        p.setName("Test " + System.nanoTime());
        p.setLocation("Test Location");
        return p;
    }

    private Task newTask(CustomerProject p, LocalDate ef) {
        Task t = new Task();
        t.setTitle("T-" + System.nanoTime());
        t.setProject(p);
        t.setStatus(Task.TaskStatus.PENDING);
        t.setPriority(Task.TaskPriority.MEDIUM);
        t.setDueDate(LocalDate.now().plusDays(30));
        t.setEfDate(ef);
        return t;
    }
}
