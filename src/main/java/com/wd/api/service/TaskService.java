package com.wd.api.service;

import com.wd.api.model.Task;
import com.wd.api.model.User;
import com.wd.api.repository.TaskRepository;
import com.wd.api.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class TaskService {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    public List<Task> getAllTasks() {
        return taskRepository.findAll();
    }

    public Optional<Task> getTaskById(Long id) {
        return taskRepository.findById(id);
    }

    public List<Task> getTasksByAssignedUser(Long userId) {
        Optional<User> user = userRepository.findById(userId);
        return user.map(taskRepository::findByAssignedTo).orElse(List.of());
    }

    public List<Task> getTasksByStatus(Task.TaskStatus status) {
        return taskRepository.findByStatus(status);
    }

    public List<Task> getTasksByProject(Long projectId) {
        return taskRepository.findByProjectId(projectId);
    }

    @Transactional
    public Task createTask(Task task, User createdBy) {
        task.setCreatedBy(createdBy);
        return taskRepository.save(task);
    }

    @Transactional
    public Task updateTask(Long id, Task taskDetails) {
        Optional<Task> existingTask = taskRepository.findById(id);
        if (existingTask.isPresent()) {
            Task task = existingTask.get();
            task.setTitle(taskDetails.getTitle());
            task.setDescription(taskDetails.getDescription());
            task.setStatus(taskDetails.getStatus());
            task.setPriority(taskDetails.getPriority());
            task.setDueDate(taskDetails.getDueDate());
            if (taskDetails.getAssignedTo() != null) {
                task.setAssignedTo(taskDetails.getAssignedTo());
            }
            if (taskDetails.getProject() != null) {
                task.setProject(taskDetails.getProject());
            }
            return taskRepository.save(task);
        }
        throw new RuntimeException("Task not found with id: " + id);
    }

    @Transactional
    public Task assignTask(Long taskId, Long userId) {
        Optional<Task> taskOpt = taskRepository.findById(taskId);
        Optional<User> userOpt = userRepository.findById(userId);

        if (taskOpt.isPresent() && userOpt.isPresent()) {
            Task task = taskOpt.get();
            task.setAssignedTo(userOpt.get());
            return taskRepository.save(task);
        }
        throw new RuntimeException("Task or User not found");
    }

    @Transactional
    public void deleteTask(Long id) {
        taskRepository.deleteById(id);
    }

    public List<Task> getMyTasks(User user) {
        return taskRepository.findByAssignedToOrderByDueDateAsc(user);
    }
}
