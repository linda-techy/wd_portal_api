package com.wd.api.repository;

import com.wd.api.model.Task;
import com.wd.api.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByAssignedTo(User user);

    List<Task> findByCreatedBy(User user);

    List<Task> findByProjectId(Long projectId);

    List<Task> findByStatus(Task.TaskStatus status);

    List<Task> findByAssignedToAndStatus(User user, Task.TaskStatus status);

    List<Task> findByAssignedToOrderByDueDateAsc(User user);
}
