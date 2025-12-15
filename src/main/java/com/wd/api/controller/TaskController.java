package com.wd.api.controller;

import com.wd.api.model.Task;
import com.wd.api.model.User;
import com.wd.api.service.TaskService;
import com.wd.api.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/tasks")
@CrossOrigin(origins = "*")
public class TaskController {

    @Autowired
    private TaskService taskService;

    @Autowired
    private UserRepository userRepository;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<Task>> getAllTasks(Authentication authentication) {
        String email = authentication.getName();

        // Check if user has ADMIN role
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (isAdmin) {
            return ResponseEntity.ok(taskService.getAllTasks());
        } else {
            // Get user by email and return their tasks
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            return ResponseEntity.ok(taskService.getTasksByAssignedUser(user.getId()));
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Task> getTaskById(@PathVariable Long id) {
        Optional<Task> task = taskService.getTaskById(id);
        return task.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/my-tasks")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<Task>> getMyTasks(Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(taskService.getMyTasks(user));
    }

    @GetMapping("/by-status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<Task>> getTasksByStatus(@PathVariable String status) {
        try {
            Task.TaskStatus taskStatus = Task.TaskStatus.valueOf(status.toUpperCase());
            return ResponseEntity.ok(taskService.getTasksByStatus(taskStatus));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/by-project/{projectId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<Task>> getTasksByProject(@PathVariable Long projectId) {
        return ResponseEntity.ok(taskService.getTasksByProject(projectId));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Task> createTask(@RequestBody Task task, Authentication authentication) {
        String email = authentication.getName();
        User createdBy = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Task createdTask = taskService.createTask(task, createdBy);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdTask);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Task> updateTask(@PathVariable Long id, @RequestBody Task task) {
        try {
            Task updatedTask = taskService.updateTask(id, task);
            return ResponseEntity.ok(updatedTask);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}/assign")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Task> assignTask(
            @PathVariable Long id,
            @RequestBody Map<String, Long> request) {
        try {
            Long userId = request.get("userId");
            Task assignedTask = taskService.assignTask(id, userId);
            return ResponseEntity.ok(assignedTask);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }
}
