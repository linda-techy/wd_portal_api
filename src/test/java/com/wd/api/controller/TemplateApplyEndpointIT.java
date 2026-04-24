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
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for POST /api/projects/{id}/templates/apply.
 *
 * <p>Verifies:
 * <ul>
 *   <li>SINGLE_FLOOR apply creates milestones for an empty project</li>
 *   <li>Second apply on a project that already has milestones is a no-op</li>
 *   <li>Unknown template code returns 400</li>
 *   <li>G_PLUS_N with floors=2 creates floor-cloned milestones</li>
 * </ul>
 *
 * <p>Runtime: Docker-deferred (requires running Testcontainers Postgres).
 * The test compiles and documents intent; IT body executes when Docker is available.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TemplateApplyEndpointIT extends TestcontainersPostgresBase {

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    TestDataSeeder seeder;

    @Autowired
    PermissionRepository permissionRepository;

    @Autowired
    PortalRoleRepository portalRoleRepository;

    @Autowired
    PortalUserRepository portalUserRepository;

    @Autowired
    CustomerProjectRepository projectRepository;

    @Autowired
    ProjectTypeTemplateRepository templateRepository;

    @Autowired
    MilestoneTemplateRepository milestoneTemplateRepository;

    @Autowired
    ProjectMilestoneRepository milestoneRepository;

    AuthTestHelper auth;

    @BeforeEach
    void setUp() {
        seeder.seed();
        auth = new AuthTestHelper(restTemplate, port);
        ensureProjectEditUser();
        ensureSingleFloorTemplate();
        ensureGPlusNTemplate();
    }

    // ------------------------------------------------------------------
    // Tests
    // ------------------------------------------------------------------

    @Test
    @Order(1)
    void applySingleFloorCreatesMilestones() {
        CustomerProject project = createEmptyProject("TEST-APPLY-1", 1);
        String jwt = auth.login("template_editor@test.com", "password123");
        HttpHeaders headers = authHeaders(jwt);

        ResponseEntity<Map> response = post(
                "/api/projects/" + project.getId() + "/templates/apply",
                "{\"templateCode\":\"SINGLE_FLOOR\"}", headers);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("created")).isEqualTo(true);
        assertThat((Integer) response.getBody().get("milestoneCount")).isGreaterThan(0);
        assertThat(milestoneRepository.countByProjectId(project.getId())).isGreaterThan(0);
    }

    @Test
    @Order(2)
    void secondApplyIsNoOp() {
        CustomerProject project = createEmptyProject("TEST-APPLY-2", 1);
        String jwt = auth.login("template_editor@test.com", "password123");
        HttpHeaders headers = authHeaders(jwt);

        // First apply
        post("/api/projects/" + project.getId() + "/templates/apply",
                "{\"templateCode\":\"SINGLE_FLOOR\"}", headers);

        // Second apply — must be no-op
        ResponseEntity<Map> response = post(
                "/api/projects/" + project.getId() + "/templates/apply",
                "{\"templateCode\":\"SINGLE_FLOOR\"}", headers);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("created")).isEqualTo(false);
    }

    @Test
    @Order(3)
    void invalidTemplateCodeReturns400() {
        CustomerProject project = createEmptyProject("TEST-APPLY-3", 1);
        String jwt = auth.login("template_editor@test.com", "password123");
        HttpHeaders headers = authHeaders(jwt);

        ResponseEntity<Map> response = post(
                "/api/projects/" + project.getId() + "/templates/apply",
                "{\"templateCode\":\"BOGUS\"}", headers);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @Order(4)
    void gPlusNClonesPerFloor() {
        // Project with floors=2: Ground + 1st Floor + 2nd Floor = 3 "Floor *" clones + 1 Handover = 4
        CustomerProject project = createEmptyProject("TEST-APPLY-4", 2);
        String jwt = auth.login("template_editor@test.com", "password123");
        HttpHeaders headers = authHeaders(jwt);

        ResponseEntity<Map> response = post(
                "/api/projects/" + project.getId() + "/templates/apply",
                "{\"templateCode\":\"G_PLUS_N\"}", headers);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("created")).isEqualTo(true);
        // G_PLUS_N template has "Floor Walls" (cloned 3×) + "Handover" (1×) = 4
        assertThat((Integer) response.getBody().get("milestoneCount")).isEqualTo(4);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

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
    private ResponseEntity<Map> post(String path, String body, HttpHeaders headers) {
        return restTemplate.exchange(
                baseUrl(path), HttpMethod.POST,
                new HttpEntity<>(body, headers), Map.class);
    }

    /**
     * Creates a project with no milestones so apply() starts from a clean state.
     */
    private CustomerProject createEmptyProject(String code, int floors) {
        return projectRepository.findAll().stream()
                .filter(p -> code.equals(p.getCode()))
                .findFirst()
                .orElseGet(() -> {
                    CustomerProject p = new CustomerProject();
                    p.setProjectUuid(UUID.randomUUID());
                    p.setCode(code);
                    p.setName("Template Apply Test " + code);
                    p.setLocation("Test Location");
                    p.setProjectType("RESIDENTIAL");
                    p.setFloors(floors);
                    p.setBudget(BigDecimal.valueOf(1_000_000));
                    p.setSqfeet(BigDecimal.valueOf(1000));
                    p.setCustomer(seeder.getCustomerA());
                    return projectRepository.save(p);
                });
    }

    /**
     * Ensures a SINGLE_FLOOR template exists with at least one milestone template.
     */
    private void ensureSingleFloorTemplate() {
        ProjectTypeTemplate template = templateRepository.findByCode("SINGLE_FLOOR").orElseGet(() -> {
            ProjectTypeTemplate t = new ProjectTypeTemplate();
            t.setCode("SINGLE_FLOOR");
            t.setProjectType("Single Floor Residential");
            t.setCategory("RESIDENTIAL");
            t.setDescription("Single floor house template");
            return templateRepository.save(t);
        });

        if (milestoneTemplateRepository.countByTemplateId(template.getId()) == 0) {
            MilestoneTemplate mt1 = new MilestoneTemplate(template, "Site Preparation", 1,
                    BigDecimal.valueOf(10), "Site clearing and leveling", "EXECUTION");
            MilestoneTemplate mt2 = new MilestoneTemplate(template, "Foundation", 2,
                    BigDecimal.valueOf(20), "Foundation work", "EXECUTION");
            milestoneTemplateRepository.save(mt1);
            milestoneTemplateRepository.save(mt2);
        }
    }

    /**
     * Ensures a G_PLUS_N template exists with one "Floor *" milestone and one non-floor milestone.
     */
    private void ensureGPlusNTemplate() {
        ProjectTypeTemplate template = templateRepository.findByCode("G_PLUS_N").orElseGet(() -> {
            ProjectTypeTemplate t = new ProjectTypeTemplate();
            t.setCode("G_PLUS_N");
            t.setProjectType("G+N Multi-Floor Residential");
            t.setCategory("RESIDENTIAL");
            t.setDescription("Multi-floor residential template");
            return templateRepository.save(t);
        });

        if (milestoneTemplateRepository.countByTemplateId(template.getId()) == 0) {
            MilestoneTemplate floorWalls = new MilestoneTemplate(template, "Floor Walls", 1,
                    BigDecimal.valueOf(15), "Floor wall construction", "EXECUTION");
            MilestoneTemplate handover = new MilestoneTemplate(template, "Handover", 2,
                    BigDecimal.valueOf(5), "Project handover", "HANDOVER");
            milestoneTemplateRepository.save(floorWalls);
            milestoneTemplateRepository.save(handover);
        }
    }

    /**
     * Ensures PROJECT_EDIT permission and a dedicated portal user exist.
     */
    private void ensureProjectEditUser() {
        Permission projectEdit = permissionRepository.findByName("PROJECT_EDIT").orElseGet(() -> {
            Permission p = new Permission();
            p.setName("PROJECT_EDIT");
            p.setDescription("Edit project details and apply templates");
            return permissionRepository.save(p);
        });

        PortalRole editorRole = portalRoleRepository.findByCode("TEMPLATE_EDITOR").orElseGet(() -> {
            PortalRole r = new PortalRole();
            r.setName("Template Editor");
            r.setCode("TEMPLATE_EDITOR");
            r.setDescription("Role with template apply capability");
            r.setPermissions(Set.of(projectEdit));
            return portalRoleRepository.save(r);
        });

        if (!editorRole.getPermissions().contains(projectEdit)) {
            editorRole.getPermissions().add(projectEdit);
            portalRoleRepository.save(editorRole);
        }

        String encoded = new BCryptPasswordEncoder(12).encode("password123");
        portalUserRepository.findByEmail("template_editor@test.com").orElseGet(() -> {
            PortalUser u = new PortalUser();
            u.setEmail("template_editor@test.com");
            u.setPassword(encoded);
            u.setFirstName("Template");
            u.setLastName("Editor");
            u.setRole(editorRole);
            u.setEnabled(true);
            return portalUserRepository.save(u);
        });
    }
}
