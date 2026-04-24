package com.wd.api.controller;

import com.wd.api.config.TestDataSeeder;
import com.wd.api.model.*;
import com.wd.api.repository.*;
import com.wd.api.support.AuthTestHelper;
import com.wd.api.testsupport.TestcontainersPostgresBase;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for PATCH /api/projects/{projectId}/tasks/{taskId}/progress.
 *
 * <p>Verifies:
 * <ul>
 *   <li>0 → 50 auto-transitions task status to IN_PROGRESS</li>
 *   <li>progress=100 auto-transitions to COMPLETED</li>
 *   <li>progressPercent > 100 returns 400</li>
 *   <li>unknown taskId returns 404</li>
 *   <li>caller without TASK_EDIT authority gets 403</li>
 * </ul>
 *
 * <p>Seeding strategy:
 * <ul>
 *   <li>Extends {@link TestcontainersPostgresBase} for a shared Postgres container.</li>
 *   <li>Calls {@link TestDataSeeder#seed()} for baseline roles/users/projects.</li>
 *   <li>Adds TASK_EDIT + TASK_VIEW permissions and TASK_STATUS_CHANGED activity type
 *       that are not in the baseline seeder but are required by this endpoint.</li>
 *   <li>Creates a dedicated portal user (task_editor@test.com) with TASK_EDIT
 *       permission; the VIEWER-only user uses SITE_ENGINEER who lacks TASK_EDIT.</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TaskProgressEndpointIT extends TestcontainersPostgresBase {

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    TestDataSeeder seeder;

    /* Repositories needed for task-specific seeding */
    @Autowired
    PermissionRepository permissionRepository;

    @Autowired
    PortalRoleRepository portalRoleRepository;

    @Autowired
    PortalUserRepository portalUserRepository;

    @Autowired
    ActivityTypeRepository activityTypeRepository;

    @Autowired
    TaskRepository taskRepository;

    @Autowired
    ProjectMilestoneRepository milestoneRepository;

    AuthTestHelper auth;

    /** Shared across ordered tests — the project used for task insertion. */
    private static Long sharedProjectId;

    @BeforeEach
    void setUp() {
        seeder.seed();
        auth = new AuthTestHelper(restTemplate, port);
        ensureTaskEditPermissions();
        ensureTaskStatusChangedActivityType();
        if (sharedProjectId == null) {
            sharedProjectId = seeder.getResidentialProject().getId();
        }
    }

    // ------------------------------------------------------------------
    // Tests
    // ------------------------------------------------------------------

    @Test
    @Order(1)
    void zeroToFiftyTransitionsToInProgress() {
        Long taskId = createTask(sharedProjectId, 0);
        String jwt = auth.login("taskeditor@test.com", "password123");
        HttpHeaders headers = authHeaders(jwt);

        ResponseEntity<Map> response = patch(
                "/api/projects/" + sharedProjectId + "/tasks/" + taskId + "/progress",
                "{\"progressPercent\": 50}", headers);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("status")).isEqualTo("IN_PROGRESS");
        assertThat(response.getBody().get("progressPercent")).isEqualTo(50);
    }

    @Test
    @Order(2)
    void fiftyToHundredTransitionsToCompleted() {
        Long taskId = createTask(sharedProjectId, 50);
        String jwt = auth.login("taskeditor@test.com", "password123");
        HttpHeaders headers = authHeaders(jwt);

        ResponseEntity<Map> response = patch(
                "/api/projects/" + sharedProjectId + "/tasks/" + taskId + "/progress",
                "{\"progressPercent\": 100}", headers);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("status")).isEqualTo("COMPLETED");
    }

    @Test
    @Order(3)
    void invalidProgressReturns400() {
        Long taskId = createTask(sharedProjectId, 0);
        String jwt = auth.login("taskeditor@test.com", "password123");
        HttpHeaders headers = authHeaders(jwt);

        ResponseEntity<Map> response = patch(
                "/api/projects/" + sharedProjectId + "/tasks/" + taskId + "/progress",
                "{\"progressPercent\": 150}", headers);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @Order(4)
    void unknownTaskReturns404() {
        String jwt = auth.login("taskeditor@test.com", "password123");
        HttpHeaders headers = authHeaders(jwt);

        ResponseEntity<Map> response = patch(
                "/api/projects/" + sharedProjectId + "/tasks/999999/progress",
                "{\"progressPercent\": 50}", headers);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @Order(5)
    void callerWithoutTaskEditReturns403() {
        Long taskId = createTask(sharedProjectId, 0);
        // SITE_ENGINEER does not have TASK_EDIT in baseline seeder
        String viewerJwt = auth.loginAsEngineer();
        HttpHeaders headers = authHeaders(viewerJwt);

        ResponseEntity<Map> response = patch(
                "/api/projects/" + sharedProjectId + "/tasks/" + taskId + "/progress",
                "{\"progressPercent\": 50}", headers);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @Order(6)
    void updatingTaskProgressRecomputesMilestoneProgress() {
        // Setup: project with a COMPUTED milestone and two tasks at 0%.
        SetupWithMilestone s = setupProjectWithMilestoneAndTwoTasks();
        String jwt = auth.login("taskeditor@test.com", "password123");
        HttpHeaders headers = authHeaders(jwt);

        // PATCH task1 to 50. Unit-weight rollup over [50, 0] = 25.
        patch(taskProgressPath(s.projectId(), s.task1Id()),
                "{\"progressPercent\": 50}", headers);

        BigDecimal milestonePct = milestoneRepository.findById(s.milestoneId())
                .orElseThrow()
                .getCompletionPercentage();
        assertThat(milestonePct).isEqualByComparingTo(new BigDecimal("25.00"));

        // PATCH task2 to 100. Unit-weight rollup over [50, 100] = 75.
        patch(taskProgressPath(s.projectId(), s.task2Id()),
                "{\"progressPercent\": 100}", headers);

        milestonePct = milestoneRepository.findById(s.milestoneId())
                .orElseThrow()
                .getCompletionPercentage();
        assertThat(milestonePct).isEqualByComparingTo(new BigDecimal("75.00"));
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    record SetupWithMilestone(Long projectId, Long milestoneId, Long task1Id, Long task2Id) {}

    /**
     * Creates a project milestone with progressSource=COMPUTED and two tasks linked to it,
     * both starting at 0% progress. Mirrors the createTask() pattern.
     */
    private SetupWithMilestone setupProjectWithMilestoneAndTwoTasks() {
        var project = seeder.getResidentialProject();
        var admin = seeder.getAdmin();

        ProjectMilestone milestone = ProjectMilestone.builder()
                .project(project)
                .name("Integration Test Milestone " + System.nanoTime())
                .amount(BigDecimal.valueOf(10000))
                .progressSource("COMPUTED")
                .completionPercentage(BigDecimal.ZERO)
                .build();
        Long milestoneId = milestoneRepository.save(milestone).getId();

        Task task1 = new Task();
        task1.setTitle("Milestone Task A " + System.nanoTime());
        task1.setDescription("Milestone IT task");
        task1.setStatus(Task.TaskStatus.PENDING);
        task1.setPriority(Task.TaskPriority.MEDIUM);
        task1.setProgressPercent(0);
        task1.setProject(project);
        task1.setCreatedBy(admin);
        task1.setAssignedTo(admin);
        task1.setDueDate(LocalDate.now().plusDays(30));
        task1.setCustomerVisible(true);
        task1.setMilestoneId(milestoneId);
        Long task1Id = taskRepository.save(task1).getId();

        Task task2 = new Task();
        task2.setTitle("Milestone Task B " + System.nanoTime());
        task2.setDescription("Milestone IT task");
        task2.setStatus(Task.TaskStatus.PENDING);
        task2.setPriority(Task.TaskPriority.MEDIUM);
        task2.setProgressPercent(0);
        task2.setProject(project);
        task2.setCreatedBy(admin);
        task2.setAssignedTo(admin);
        task2.setDueDate(LocalDate.now().plusDays(30));
        task2.setCustomerVisible(true);
        task2.setMilestoneId(milestoneId);
        Long task2Id = taskRepository.save(task2).getId();

        return new SetupWithMilestone(project.getId(), milestoneId, task1Id, task2Id);
    }

    /** Builds the progress PATCH path for a given project + task. */
    private String taskProgressPath(Long projectId, Long taskId) {
        return "/api/projects/" + projectId + "/tasks/" + taskId + "/progress";
    }

    private String baseUrl(String path) {
        return "http://localhost:" + port + path;
    }

    private HttpHeaders authHeaders(String token) {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(token);
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }

    @SuppressWarnings("unchecked")
    private ResponseEntity<Map> patch(String path, String body, HttpHeaders headers) {
        return restTemplate.exchange(
                baseUrl(path), HttpMethod.PATCH,
                new HttpEntity<>(body, headers), Map.class);
    }

    /**
     * Creates a task in the given project with the specified initial progress.
     * Status is derived automatically (0=PENDING, 1-99=IN_PROGRESS, 100=COMPLETED).
     */
    private Long createTask(Long projectId, int initialProgress) {
        var project = seeder.getResidentialProject();
        var admin = seeder.getAdmin();

        Task task = new Task();
        task.setTitle("Test Task " + System.nanoTime());
        task.setDescription("Created by TaskProgressEndpointIT");
        task.setStatus(initialProgress == 0 ? Task.TaskStatus.PENDING
                : initialProgress == 100 ? Task.TaskStatus.COMPLETED
                        : Task.TaskStatus.IN_PROGRESS);
        task.setPriority(Task.TaskPriority.MEDIUM);
        task.setProgressPercent(initialProgress);
        task.setProject(project);
        task.setCreatedBy(admin);
        task.setAssignedTo(admin);
        task.setDueDate(LocalDate.now().plusDays(30));
        task.setCustomerVisible(true);
        return taskRepository.save(task).getId();
    }

    /**
     * Ensures TASK_EDIT and TASK_VIEW permissions exist, and that a dedicated
     * portal role/user with TASK_EDIT is created. Idempotent.
     */
    private void ensureTaskEditPermissions() {
        Permission taskEdit = permissionRepository.findByName("TASK_EDIT").orElseGet(() -> {
            Permission p = new Permission();
            p.setName("TASK_EDIT");
            p.setDescription("Edit task progress and details");
            return permissionRepository.save(p);
        });

        Permission taskView = permissionRepository.findByName("TASK_VIEW").orElseGet(() -> {
            Permission p = new Permission();
            p.setName("TASK_VIEW");
            p.setDescription("View tasks");
            return permissionRepository.save(p);
        });

        PortalRole taskEditorRole = portalRoleRepository.findByCode("TASK_EDITOR").orElseGet(() -> {
            PortalRole r = new PortalRole();
            r.setName("Task Editor");
            r.setCode("TASK_EDITOR");
            r.setDescription("Role with task edit capability");
            r.setPermissions(Set.of(taskEdit, taskView));
            return portalRoleRepository.save(r);
        });
        // ensure permissions are present even if role already existed
        if (!taskEditorRole.getPermissions().contains(taskEdit)) {
            taskEditorRole.getPermissions().add(taskEdit);
            taskEditorRole.getPermissions().add(taskView);
            portalRoleRepository.save(taskEditorRole);
        }

        // Create the task editor portal user.
        // Email must not contain underscores: JwtService.extractActualSubject() strips the
        // legacy "TYPE_" prefix by cutting at the first '_', which would corrupt an email like
        // "task_editor@test.com" into "editor@test.com" and cause 401 on every subsequent request.
        String encoded = new BCryptPasswordEncoder(12).encode("password123");
        portalUserRepository.findByEmail("taskeditor@test.com").orElseGet(() -> {
            PortalUser u = new PortalUser();
            u.setEmail("taskeditor@test.com");
            u.setPassword(encoded);
            u.setFirstName("Task");
            u.setLastName("Editor");
            u.setRole(taskEditorRole);
            u.setEnabled(true);
            return portalUserRepository.save(u);
        });
    }

    /**
     * Ensures TASK_STATUS_CHANGED activity type exists for ActivityFeedService.
     * Without this, the service throws RuntimeException when logging status transitions.
     */
    private void ensureTaskStatusChangedActivityType() {
        activityTypeRepository.findByName("TASK_STATUS_CHANGED").orElseGet(() -> {
            ActivityType at = new ActivityType();
            at.setName("TASK_STATUS_CHANGED");
            at.setDescription("Task status changed via progress update");
            return activityTypeRepository.save(at);
        });
    }
}
