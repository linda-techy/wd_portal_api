package com.wd.api.controller;

import com.wd.api.config.TestDataSeeder;
import com.wd.api.model.BoqCategory;
import com.wd.api.model.BoqDocument;
import com.wd.api.model.BoqItem;
import com.wd.api.model.CustomerProject;
import com.wd.api.model.CustomerUser;
import com.wd.api.model.DpcScopeOption;
import com.wd.api.model.DpcScopeTemplate;
import com.wd.api.model.Permission;
import com.wd.api.model.PortalRole;
import com.wd.api.model.PortalUser;
import com.wd.api.model.enums.BoqDocumentStatus;
import com.wd.api.model.enums.ItemKind;
import com.wd.api.repository.BoqCategoryRepository;
import com.wd.api.repository.BoqDocumentRepository;
import com.wd.api.repository.BoqItemRepository;
import com.wd.api.repository.CustomerProjectRepository;
import com.wd.api.repository.DpcDocumentRepository;
import com.wd.api.repository.DpcScopeOptionRepository;
import com.wd.api.repository.DpcScopeTemplateRepository;
import com.wd.api.repository.PermissionRepository;
import com.wd.api.repository.PortalRoleRepository;
import com.wd.api.repository.PortalUserRepository;
import com.wd.api.support.AuthTestHelper;
import com.wd.api.testsupport.TestcontainersPostgresBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the DPC document REST endpoints.
 *
 * <p>Verifies the create / patch / preview / issue happy path against a real
 * Postgres container.  Scope-template seed rows are inserted by hand because
 * Flyway is disabled in the test container (V68 migration is the production
 * source of truth, but the test runs Hibernate {@code create-drop} on bare
 * entity tables).
 *
 * <p>Skipped for v1: ACL-failure permutations, DELETE customization, admin
 * scope-template endpoints. Those would test {@code @PreAuthorize} wiring or
 * branches we cover at the service-layer unit-test level.
 *
 * <p>Runtime: Docker-deferred (requires running Testcontainers Postgres).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DpcDocumentEndpointIT extends TestcontainersPostgresBase {

    private static final String DPC_USER_EMAIL = "dpc_tester@test.com";
    private static final String DPC_USER_PASSWORD = "password123";
    private static final String DPC_ROLE_CODE = "DPC_TESTER";

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
    BoqDocumentRepository boqDocumentRepository;

    @Autowired
    BoqItemRepository boqItemRepository;

    @Autowired
    BoqCategoryRepository boqCategoryRepository;

    @Autowired
    DpcDocumentRepository dpcDocumentRepository;

    @Autowired
    DpcScopeTemplateRepository dpcScopeTemplateRepository;

    @Autowired
    DpcScopeOptionRepository dpcScopeOptionRepository;

    AuthTestHelper auth;

    @BeforeEach
    void setUp() {
        seeder.seed();
        auth = new AuthTestHelper(restTemplate, port);
        ensureDpcUser();
        ensureScopeTemplatesSeeded();
    }

    // ------------------------------------------------------------------
    // Tests
    // ------------------------------------------------------------------

    @Test
    @Order(1)
    void createDpc_returnsDraft_withRevision1_andSeededScopes() {
        CustomerProject project = createFreshProject("DPC-CREATE-1");
        BoqDocument boq = createApprovedBoq(project);
        BoqCategory category = createFoundationCategory(project);
        createBasicBoqItems(project, boq, category);

        String jwt = auth.login(DPC_USER_EMAIL, DPC_USER_PASSWORD);
        HttpHeaders headers = authHeaders(jwt);

        ResponseEntity<Map> response = post(
                "/api/projects/" + project.getId() + "/dpc-documents", "{}", headers);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        assertThat(data).isNotNull();
        assertThat(data.get("status")).isEqualTo("DRAFT");
        assertThat(((Number) data.get("revisionNumber")).intValue()).isEqualTo(1);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> scopes = (List<Map<String, Object>>) data.get("scopes");
        assertThat(scopes).isNotNull();
        // V68 seeds 10 templates; we seed all 10 in ensureScopeTemplatesSeeded.
        assertThat(scopes.size()).isGreaterThanOrEqualTo(10);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> lines = (List<Map<String, Object>>) data.get("customizationLines");
        // Auto-populate created at least the 1 ADDON we seeded.
        assertThat(lines).isNotNull();
        assertThat(lines.size()).isGreaterThanOrEqualTo(1);

        @SuppressWarnings("unchecked")
        Map<String, Object> summary = (Map<String, Object>) data.get("masterCostSummary");
        assertThat(summary).isNotNull();
        BigDecimal totalOriginal = toBigDecimal(summary.get("totalOriginal"));
        BigDecimal totalCustomized = toBigDecimal(summary.get("totalCustomized"));
        assertThat(totalOriginal).isGreaterThan(BigDecimal.ZERO);
        assertThat(totalCustomized).isGreaterThan(totalOriginal);
    }

    @Test
    @Order(2)
    void createDpc_returns422_whenBoqNotApproved() {
        CustomerProject project = createFreshProject("DPC-CREATE-NOAPP");
        // BoQ in DRAFT — DPC creation requires APPROVED.
        BoqDocument boq = new BoqDocument();
        boq.setProject(project);
        boq.setStatus(BoqDocumentStatus.DRAFT);
        boqDocumentRepository.save(boq);

        String jwt = auth.login(DPC_USER_EMAIL, DPC_USER_PASSWORD);
        HttpHeaders headers = authHeaders(jwt);

        ResponseEntity<Map> response = post(
                "/api/projects/" + project.getId() + "/dpc-documents", "{}", headers);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    @Order(3)
    void patchScope_updatesSelection() {
        CustomerProject project = createFreshProject("DPC-PATCH-SCOPE");
        BoqDocument boq = createApprovedBoq(project);
        BoqCategory category = createFoundationCategory(project);
        createBasicBoqItems(project, boq, category);

        String jwt = auth.login(DPC_USER_EMAIL, DPC_USER_PASSWORD);
        HttpHeaders headers = authHeaders(jwt);

        ResponseEntity<Map> create = post(
                "/api/projects/" + project.getId() + "/dpc-documents", "{}", headers);
        assertThat(create.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        @SuppressWarnings("unchecked")
        Map<String, Object> dpc = (Map<String, Object>) create.getBody().get("data");
        Long dpcId = ((Number) dpc.get("id")).longValue();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> scopes = (List<Map<String, Object>>) dpc.get("scopes");
        Map<String, Object> foundationScope = scopes.stream()
                .filter(s -> "FOUNDATION".equals(s.get("scopeCode")))
                .findFirst().orElseThrow(() -> new AssertionError("FOUNDATION scope row not seeded"));
        Long scopeRowId = ((Number) foundationScope.get("id")).longValue();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> options = (List<Map<String, Object>>) foundationScope.get("availableOptions");
        assertThat(options).isNotEmpty();
        Long optionId = ((Number) options.get(0).get("id")).longValue();

        String body = "{\"selectedOptionId\":" + optionId
                + ",\"selectedOptionRationale\":\"Soil test confirmed bearing.\"}";
        ResponseEntity<Map> patch = exchange(
                "/api/dpc-documents/" + dpcId + "/scopes/" + scopeRowId,
                HttpMethod.PATCH, body, headers);

        assertThat(patch.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, Object> updated = (Map<String, Object>) patch.getBody().get("data");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> updatedScopes = (List<Map<String, Object>>) updated.get("scopes");
        Map<String, Object> updatedFoundation = updatedScopes.stream()
                .filter(s -> ((Number) s.get("id")).longValue() == scopeRowId)
                .findFirst().orElseThrow();
        assertThat(updatedFoundation.get("selectedOptionRationale"))
                .isEqualTo("Soil test confirmed bearing.");
        assertThat(((Number) updatedFoundation.get("selectedOptionId")).longValue()).isEqualTo(optionId);
    }

    @Test
    @Order(4)
    void previewPdf_returnsPdfBytes() {
        CustomerProject project = createFreshProject("DPC-PREVIEW");
        BoqDocument boq = createApprovedBoq(project);
        BoqCategory category = createFoundationCategory(project);
        createBasicBoqItems(project, boq, category);

        String jwt = auth.login(DPC_USER_EMAIL, DPC_USER_PASSWORD);
        HttpHeaders headers = authHeaders(jwt);

        ResponseEntity<Map> create = post(
                "/api/projects/" + project.getId() + "/dpc-documents", "{}", headers);
        assertThat(create.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        @SuppressWarnings("unchecked")
        Map<String, Object> dpc = (Map<String, Object>) create.getBody().get("data");
        Long dpcId = ((Number) dpc.get("id")).longValue();

        // Preview doesn't have an ApiResponse wrapper — body is raw PDF bytes.
        ResponseEntity<byte[]> response = restTemplate.exchange(
                baseUrl("/api/dpc-documents/" + dpcId + "/preview-pdf"),
                HttpMethod.GET, new HttpEntity<>(headers), byte[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        MediaType contentType = response.getHeaders().getContentType();
        assertThat(contentType).isNotNull();
        assertThat(contentType.toString()).contains("application/pdf");
        assertThat(response.getBody()).isNotNull();
        // Sanity: a real PDF rendered from the full DPC template must exceed 1KB.
        assertThat(response.getBody().length).isGreaterThan(1000);
    }

    @Test
    @Order(5)
    void issueDpc_flipsStatus_andPersistsDocument() {
        CustomerProject project = createFreshProject("DPC-ISSUE");
        BoqDocument boq = createApprovedBoq(project);
        BoqCategory category = createFoundationCategory(project);
        createBasicBoqItems(project, boq, category);

        String jwt = auth.login(DPC_USER_EMAIL, DPC_USER_PASSWORD);
        HttpHeaders headers = authHeaders(jwt);

        ResponseEntity<Map> create = post(
                "/api/projects/" + project.getId() + "/dpc-documents", "{}", headers);
        assertThat(create.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        @SuppressWarnings("unchecked")
        Map<String, Object> dpc = (Map<String, Object>) create.getBody().get("data");
        Long dpcId = ((Number) dpc.get("id")).longValue();

        ResponseEntity<Map> issue = post(
                "/api/dpc-documents/" + dpcId + "/issue", "{}", headers);

        assertThat(issue.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, Object> issued = (Map<String, Object>) issue.getBody().get("data");
        assertThat(issued.get("status")).isEqualTo("ISSUED");
        assertThat(issued.get("issuedAt")).isNotNull();
        assertThat(issued.get("issuedPdfDocumentId")).isNotNull();
    }

    // ------------------------------------------------------------------
    // Test data helpers
    // ------------------------------------------------------------------

    private CustomerProject createFreshProject(String code) {
        return projectRepository.findAll().stream()
                .filter(p -> code.equals(p.getCode()))
                .findFirst()
                .orElseGet(() -> {
                    CustomerProject p = new CustomerProject();
                    p.setProjectUuid(UUID.randomUUID());
                    p.setCode(code);
                    p.setName("Test Project");
                    p.setLocation("Test Location");
                    p.setProjectType("RESIDENTIAL");
                    p.setSqfeet(BigDecimal.valueOf(2012));
                    p.setBudget(BigDecimal.valueOf(1_000_000));
                    p.setCustomer(seeder.getCustomerA());
                    p.setProjectManager(seeder.getProjectManager());
                    return projectRepository.save(p);
                });
    }

    /**
     * Create an APPROVED BoQ document linked to the project. The
     * {@code approvedAt} stamp is required by
     * {@code findFirstByProject_IdAndStatusOrderByApprovedAtDesc} which the
     * DPC create flow uses.
     */
    private BoqDocument createApprovedBoq(CustomerProject project) {
        BoqDocument boq = new BoqDocument();
        boq.setProject(project);
        boq.setStatus(BoqDocumentStatus.APPROVED);
        boq.setApprovedAt(LocalDateTime.now());
        boq.setRevisionNumber(1);
        return boqDocumentRepository.save(boq);
    }

    /**
     * Create a BoQ category named "Foundation Works" — its name contains
     * "foundation" so it matches the FOUNDATION scope template's pattern list
     * and the BoQ items below roll up into the Foundation scope.
     */
    private BoqCategory createFoundationCategory(CustomerProject project) {
        BoqCategory category = new BoqCategory();
        category.setProject(project);
        category.setName("Foundation Works");
        category.setDescription("Foundation, footing, and earthwork");
        category.setDisplayOrder(1);
        category.setIsActive(true);
        return boqCategoryRepository.save(category);
    }

    /**
     * Create one BASE item (₹100,000) and one ADDON item (₹50,000) under the
     * given category. The ADDON drives the auto-customization line; the BASE
     * drives the "original" cost rollup.
     */
    private void createBasicBoqItems(CustomerProject project, BoqDocument boq, BoqCategory category) {
        BoqItem base = new BoqItem();
        base.setProject(project);
        base.setBoqDocument(boq);
        base.setCategory(category);
        base.setDescription("Foundation Concrete (BASE)");
        base.setUnit("CUM");
        base.setQuantity(BigDecimal.valueOf(10));
        base.setUnitRate(BigDecimal.valueOf(10_000));
        base.setTotalAmount(BigDecimal.valueOf(100_000));
        base.setItemKind(ItemKind.BASE);
        base.setIsActive(true);
        boqItemRepository.save(base);

        BoqItem addon = new BoqItem();
        addon.setProject(project);
        addon.setBoqDocument(boq);
        addon.setCategory(category);
        addon.setDescription("Extra Foundation Reinforcement (ADDON)");
        addon.setSpecifications("Customer-requested upgrade for harder soil.");
        addon.setUnit("LOT");
        addon.setQuantity(BigDecimal.ONE);
        addon.setUnitRate(BigDecimal.valueOf(50_000));
        addon.setTotalAmount(BigDecimal.valueOf(50_000));
        addon.setItemKind(ItemKind.ADDON);
        addon.setIsActive(true);
        boqItemRepository.save(addon);
    }

    /**
     * Ensure all 10 standard scope templates from V68 are present. Tests run
     * with Flyway disabled, so the production seed data isn't applied — we
     * recreate just enough here for the cost rollup and FOUNDATION-pattern
     * match to work.
     */
    private void ensureScopeTemplatesSeeded() {
        if (dpcScopeTemplateRepository.count() >= 10) return;

        Object[][] templates = new Object[][] {
                {"FOUNDATION",     1,  "Foundation.",      List.of("foundation","footing","pcc","plinth","earthwork","excavation")},
                {"SUPERSTRUCTURE", 2,  "Superstructure.",  List.of("superstructure","masonry","block","brick","concrete","slab","beam","column","rcc","wall")},
                {"PLASTERING",     3,  "Plastering.",      List.of("plaster","plastering","stucco","render")},
                {"ELECTRICAL",     4,  "Electrical.",      List.of("electrical","wiring","switch","light","mep electrical")},
                {"PLUMBING",       5,  "Plumbing.",        List.of("plumbing","sanitary","pipe","drain","mep plumbing")},
                {"FLOORING",       6,  "Flooring.",        List.of("flooring","tile","tiles","floor","skirting")},
                {"JOINERY",        7,  "Joinery.",         List.of("joinery","door","window","frame","upvc","wood")},
                {"WALL_FINISHES",  8,  "Wall finishes.",   List.of("wall finish","paint","painting","emulsion","putty")},
                {"WATERPROOFING",  9,  "Waterproofing.",   List.of("waterproof","waterproofing","membrane","damp")},
                {"ELEVATION",      10, "Elevation.",       List.of("elevation","facade","fabrication","ms work","texture")},
        };

        for (Object[] row : templates) {
            String code = (String) row[0];
            if (dpcScopeTemplateRepository.findByCode(code).isPresent()) continue;

            DpcScopeTemplate t = new DpcScopeTemplate();
            t.setCode(code);
            t.setDisplayOrder((Integer) row[1]);
            t.setTitle((String) row[2]);
            t.setSubtitle("Test subtitle for " + code);
            t.setIntroParagraph("Test intro for " + code);
            @SuppressWarnings("unchecked")
            List<String> patterns = (List<String>) row[3];
            t.setBoqCategoryPatterns(new java.util.ArrayList<>(patterns));
            t.setIsActive(true);
            DpcScopeTemplate saved = dpcScopeTemplateRepository.save(t);

            // Each template gets one option so the patchScope test has something
            // to select.  FOUNDATION only needs at least one — we add two for
            // realism and to keep the available-options list non-trivial.
            DpcScopeOption opt1 = new DpcScopeOption();
            opt1.setScopeTemplate(saved);
            opt1.setCode(code + "_OPT_1");
            opt1.setDisplayName(code + " Option 1");
            opt1.setDisplayOrder(1);
            opt1.setIsActive(true);
            dpcScopeOptionRepository.save(opt1);

            DpcScopeOption opt2 = new DpcScopeOption();
            opt2.setScopeTemplate(saved);
            opt2.setCode(code + "_OPT_2");
            opt2.setDisplayName(code + " Option 2");
            opt2.setDisplayOrder(2);
            opt2.setIsActive(true);
            dpcScopeOptionRepository.save(opt2);
        }
    }

    /**
     * Ensure a portal user with all four DPC permissions exists.  Mirrors the
     * pattern in {@code TemplateApplyEndpointIT#ensureProjectEditUser()} but
     * with a dedicated DPC_TESTER role.
     */
    private void ensureDpcUser() {
        Set<Permission> dpcPerms = new HashSet<>();
        for (String name : List.of("DPC_VIEW", "DPC_CREATE", "DPC_EDIT", "DPC_ISSUE")) {
            Permission p = permissionRepository.findByName(name).orElseGet(() -> {
                Permission np = new Permission();
                np.setName(name);
                np.setDescription("DPC permission: " + name);
                return permissionRepository.save(np);
            });
            dpcPerms.add(p);
        }

        PortalRole role = portalRoleRepository.findByCode(DPC_ROLE_CODE).orElseGet(() -> {
            PortalRole r = new PortalRole();
            r.setName("DPC Tester");
            r.setCode(DPC_ROLE_CODE);
            r.setDescription("Role with full DPC builder capabilities for IT tests");
            r.setPermissions(dpcPerms);
            return portalRoleRepository.save(r);
        });
        if (role.getPermissions() == null || !role.getPermissions().containsAll(dpcPerms)) {
            Set<Permission> merged = role.getPermissions() != null
                    ? new HashSet<>(role.getPermissions()) : new HashSet<>();
            merged.addAll(dpcPerms);
            role.setPermissions(merged);
            role = portalRoleRepository.save(role);
        }

        final PortalRole finalRole = role;
        String encoded = new BCryptPasswordEncoder(12).encode(DPC_USER_PASSWORD);
        portalUserRepository.findByEmail(DPC_USER_EMAIL).orElseGet(() -> {
            PortalUser u = new PortalUser();
            u.setEmail(DPC_USER_EMAIL);
            u.setPassword(encoded);
            u.setFirstName("DPC");
            u.setLastName("Tester");
            u.setRole(finalRole);
            u.setEnabled(true);
            return portalUserRepository.save(u);
        });
    }

    // ------------------------------------------------------------------
    // HTTP helpers
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

    @SuppressWarnings("unchecked")
    private ResponseEntity<Map> exchange(String path, HttpMethod method, String body, HttpHeaders headers) {
        return restTemplate.exchange(
                baseUrl(path), method,
                new HttpEntity<>(body, headers), Map.class);
    }

    /** Coerce Jackson's Number/Integer/Double output to BigDecimal for assertions. */
    private static BigDecimal toBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal bd) return bd;
        if (value instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        return new BigDecimal(value.toString());
    }
}
